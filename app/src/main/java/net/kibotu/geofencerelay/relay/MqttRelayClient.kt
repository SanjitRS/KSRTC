package net.kibotu.geofencerelay.relay

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kibotu.geofencerelay.model.BreachAlert
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.model.PatientCognitiveTelemetry
import net.kibotu.geofencerelay.model.RemoteCommand
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

class MqttRelayClient(
    private val brokerUrl: String = "tcp://broker.emqx.io:1883"
) : RelayClient {

    private val tag = "FindMyMqtt"
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var client: MqttClient? = null
    var currentUserEmail: String = ""
        private set
    val userEmail: String get() = currentUserEmail

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val isClientConnected: Boolean
        get() = client?.isConnected == true

    private val _activeZone = MutableStateFlow<GeofenceZone?>(null)
    override val activeZone: StateFlow<GeofenceZone?> = _activeZone.asStateFlow()

    private val _latestPing = MutableSharedFlow<LocationPing>(replay = 1, extraBufferCapacity = 64)
    override val latestPing: SharedFlow<LocationPing> = _latestPing.asSharedFlow()

    private val _breachAlert = MutableSharedFlow<BreachAlert>(replay = 1, extraBufferCapacity = 32)
    override val breachAlert: SharedFlow<BreachAlert> = _breachAlert.asSharedFlow()

    private val _incomingCommand = MutableSharedFlow<RemoteCommand>(replay = 1, extraBufferCapacity = 16)
    override val incomingCommand: SharedFlow<RemoteCommand> = _incomingCommand.asSharedFlow()

    private val publishMutex = Mutex()

    private val _latestTelemetry = MutableSharedFlow<PatientCognitiveTelemetry>(replay = 1, extraBufferCapacity = 16)
    val latestTelemetry: SharedFlow<PatientCognitiveTelemetry> = _latestTelemetry.asSharedFlow()

    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace("+", "_")
    }

    private val isConnecting = java.util.concurrent.atomic.AtomicBoolean(false)

    override suspend fun connect(userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val effective = userEmail.trim().lowercase().ifBlank { "smaran_shared" }
        currentUserEmail = effective

        // If already connected, ensure subscriptions are active
        val existing = client
        if (existing != null && existing.isConnected) {
            _isConnected.value = true
            subscribeForEmail(currentUserEmail)
            return@withContext true
        }

        // Prevent overlapping connection attempts
        if (!isConnecting.compareAndSet(false, true)) {
            return@withContext false
        }

        try {
            disconnect()
            val clientId = "findmy_${UUID.randomUUID().toString().take(8)}"
            val mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
            client = mqttClient

            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(tag, "Connected to relay (reconnect=$reconnect, server=$serverURI)")
                    _isConnected.value = true
                    subscribeForEmail(currentUserEmail)
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w(tag, "Connection lost: ${cause?.message}")
                    _isConnected.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.payload?.let { String(it) } ?: return
                    handleIncomingMessage(topic ?: "", payload)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                serverURIs = arrayOf(brokerUrl)
            }

            Log.d(tag, "Connecting to MQTT broker for $currentUserEmail...")
            mqttClient.connect(options)
            _isConnected.value = true
            subscribeForEmail(currentUserEmail)
            Log.d(tag, "Successfully connected and subscribed for $currentUserEmail")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect: ${e.message}", e)
            _isConnected.value = false
            false
        } finally {
            isConnecting.set(false)
        }
    }

    fun subscribeForEmail(email: String) {
        val c = client ?: return
        if (!c.isConnected) return
        val sanitized = sanitizeEmail(email)
        val baseTopic = "bmtc_findmy/v2/$sanitized"
        try {
            // Subscribe to primary topic
            c.subscribe("$baseTopic/#", 1)
            Log.d(tag, "Subscribed to wildcard $baseTopic/# for Account: $email")

            // Universal cross-device fallback subscriptions (ensures pairing regardless of sign-in status)
            if (sanitized != "patient_device_at_smaran_local") {
                c.subscribe("bmtc_findmy/v2/patient_device_at_smaran_local/#", 1)
            }
            if (sanitized != "guardian_device_at_smaran_local") {
                c.subscribe("bmtc_findmy/v2/guardian_device_at_smaran_local/#", 1)
            }
            c.subscribe("bmtc_findmy/v2/smaran_shared/#", 1)
            Log.d(tag, "Subscribed to universal cross-pairing channels (smaran_shared, defaults)")
        } catch (e: Exception) {
            Log.e(tag, "Subscribe error: ${e.message}", e)
        }
    }

    private fun handleIncomingMessage(topic: String, payload: String) {
        scope.launch {
            try {
                when {
                    topic.endsWith("/location") -> {
                        val ping = json.decodeFromString<LocationPing>(payload)
                        _latestPing.emit(ping)
                        Log.d(tag, "Received location from ${ping.deviceName}: ${ping.latitude}, ${ping.longitude}")
                    }
                    topic.endsWith("/zone") -> {
                        val zone = json.decodeFromString<GeofenceZone>(payload)
                        _activeZone.value = zone
                        Log.d(tag, "Received geofence zone: ${zone.name} (r=${zone.radiusMeters}m)")
                    }
                    topic.endsWith("/alert") -> {
                        val alert = json.decodeFromString<BreachAlert>(payload)
                        _breachAlert.emit(alert)
                        Log.w(tag, "Received breach alert: ${alert.status}")
                    }
                    topic.endsWith("/command") -> {
                        val cmd = json.decodeFromString<RemoteCommand>(payload)
                        _incomingCommand.emit(cmd)
                        Log.d(tag, "Received remote command: ${cmd.command}")
                    }
                    topic.endsWith("/telemetry") -> {
                        val tel = json.decodeFromString<PatientCognitiveTelemetry>(payload)
                        _latestTelemetry.emit(tel)
                        Log.d(tag, "Received patient cognitive telemetry: CPS=${tel.compositeCps}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing incoming payload on $topic: ${e.message}")
            }
        }
    }

    private suspend fun ensureConnected(email: String): Boolean {
        if (client?.isConnected == true && _isConnected.value) return true
        connect(email)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 5000L) {
            if (client?.isConnected == true && _isConnected.value) return true
            kotlinx.coroutines.delay(100L)
        }
        return client?.isConnected == true
    }

    override suspend fun publishZone(targetEmail: String, zone: GeofenceZone): Boolean = withContext(Dispatchers.IO) {
        val emailToUse = targetEmail.ifBlank { "smaran_shared" }
        val topic = "bmtc_findmy/v2/${sanitizeEmail(emailToUse)}/${zone.id}/zone"
        val payload = json.encodeToString(zone)
        if (client?.isConnected != true) {
            ensureConnected(emailToUse)
        }
        var ok = publishInternal(topic, payload, qos = 1, retained = true)
        if (!ok) {
            ensureConnected(emailToUse)
            ok = publishInternal(topic, payload, qos = 1, retained = true)
        }
        publishInternal("bmtc_findmy/v2/patient_device_at_smaran_local/${zone.id}/zone", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/guardian_device_at_smaran_local/${zone.id}/zone", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/smaran_shared/${zone.id}/zone", payload, qos = 1, retained = true)
        ok
    }

    override suspend fun publishPing(targetEmail: String, ping: LocationPing): Boolean = withContext(Dispatchers.IO) {
        val emailToUse = targetEmail.ifBlank { "smaran_shared" }
        val topic = "bmtc_findmy/v2/${sanitizeEmail(emailToUse)}/${ping.deviceId}/location"
        val payload = json.encodeToString(ping)
        if (client?.isConnected != true) {
            ensureConnected(emailToUse)
        }
        var ok = publishInternal(topic, payload, qos = 1, retained = true)
        if (!ok) {
            ensureConnected(emailToUse)
            ok = publishInternal(topic, payload, qos = 1, retained = true)
        }
        // Mirror to universal channels so any paired Caregiver receives live GPS
        publishInternal("bmtc_findmy/v2/guardian_device_at_smaran_local/${ping.deviceId}/location", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/patient_device_at_smaran_local/${ping.deviceId}/location", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/smaran_shared/${ping.deviceId}/location", payload, qos = 1, retained = true)
        ok
    }

    override suspend fun publishAlert(targetEmail: String, alert: BreachAlert): Boolean = withContext(Dispatchers.IO) {
        val emailToUse = targetEmail.ifBlank { "smaran_shared" }
        val topic = "bmtc_findmy/v2/${sanitizeEmail(emailToUse)}/${alert.deviceId}/alert"
        val payload = json.encodeToString(alert)
        if (client?.isConnected != true) {
            ensureConnected(emailToUse)
        }
        var ok = publishInternal(topic, payload, qos = 1, retained = false)
        if (!ok) {
            ensureConnected(emailToUse)
            ok = publishInternal(topic, payload, qos = 1, retained = false)
        }
        publishInternal("bmtc_findmy/v2/smaran_shared/${alert.deviceId}/alert", payload, qos = 1, retained = false)
        ok
    }

    override suspend fun sendCommand(targetEmail: String, deviceId: String, command: RemoteCommand): Boolean = withContext(Dispatchers.IO) {
        val emailToUse = targetEmail.ifBlank { "smaran_shared" }
        val topic = "bmtc_findmy/v2/${sanitizeEmail(emailToUse)}/$deviceId/command"
        val payload = json.encodeToString(command)
        if (client?.isConnected != true) {
            ensureConnected(emailToUse)
        }
        var ok = publishInternal(topic, payload, qos = 1, retained = false)
        if (!ok) {
            ensureConnected(emailToUse)
            ok = publishInternal(topic, payload, qos = 1, retained = false)
        }
        publishInternal("bmtc_findmy/v2/patient_device_at_smaran_local/$deviceId/command", payload, qos = 1, retained = false)
        publishInternal("bmtc_findmy/v2/patient_device_at_smaran_local/all/command", payload, qos = 1, retained = false)
        publishInternal("bmtc_findmy/v2/smaran_shared/$deviceId/command", payload, qos = 1, retained = false)
        publishInternal("bmtc_findmy/v2/smaran_shared/all/command", payload, qos = 1, retained = false)
        ok
    }

    suspend fun publishCognitiveTelemetry(targetEmail: String, telemetry: PatientCognitiveTelemetry): Boolean = withContext(Dispatchers.IO) {
        val emailToUse = targetEmail.ifBlank { "smaran_shared" }
        val topic = "bmtc_findmy/v2/${sanitizeEmail(emailToUse)}/patient/telemetry"
        val payload = json.encodeToString(telemetry)
        if (client?.isConnected != true) {
            ensureConnected(emailToUse)
        }
        var ok = publishInternal(topic, payload, qos = 1, retained = true)
        if (!ok) {
            ensureConnected(emailToUse)
            ok = publishInternal(topic, payload, qos = 1, retained = true)
        }
        // Mirror to universal channels so any Caregiver receives the live score immediately
        publishInternal("bmtc_findmy/v2/guardian_device_at_smaran_local/patient/telemetry", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/patient_device_at_smaran_local/patient/telemetry", payload, qos = 1, retained = true)
        publishInternal("bmtc_findmy/v2/smaran_shared/patient/telemetry", payload, qos = 1, retained = true)
        ok
    }

    private suspend fun publishInternal(topic: String, payload: String, qos: Int, retained: Boolean): Boolean = publishMutex.withLock {
        val c = client
        if (c == null || !c.isConnected) {
            Log.w(tag, "Cannot publish to $topic: disconnected")
            return@withLock false
        }
        return@withLock try {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            c.publish(topic, message)
            Log.d(tag, "Published to $topic (qos=$qos, retained=$retained)")
            true
        } catch (e: Exception) {
            Log.e(tag, "Publish failed: ${e.message}", e)
            false
        }
    }

    override fun disconnect() {
        try {
            client?.let {
                if (it.isConnected) it.disconnect()
                it.close()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting: ${e.message}")
        } finally {
            client = null
            _isConnected.value = false
        }
    }

    companion object {
        val shared: MqttRelayClient by lazy { MqttRelayClient() }
    }
}
