package net.kibotu.geofencerelay.ui.guardian

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kibotu.geofencerelay.model.BreachAlert
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.model.RemoteCommand
import net.kibotu.geofencerelay.model.GameSessionRecord
import net.kibotu.geofencerelay.model.PatientCognitiveTelemetry
import net.kibotu.geofencerelay.relay.MqttRelayClient
import net.kibotu.geofencerelay.util.BatteryUtils
import net.kibotu.geofencerelay.util.LocationUtils
import net.kibotu.geofencerelay.util.NotificationHelper
import net.kibotu.geofencerelay.util.SoundPlayer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class GuardianViewModel(application: Application) : AndroidViewModel(application) {

    private val relay = MqttRelayClient.shared
    val isConnected = relay.isConnected
    private var guardianEmail: String = ""

    private val app = application
    private val prefs = app.getSharedPreferences("guardian_target_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadCachedTelemetry(): PatientCognitiveTelemetry? {
        val raw = prefs.getString("cached_patient_telemetry", null) ?: return null
        return try {
            json.decodeFromString<PatientCognitiveTelemetry>(raw)
        } catch (_: Exception) {
            null
        }
    }

    private val _targetPatientEmail = MutableStateFlow(
        prefs.getString("target_patient_email", "patient.device@smaran.local") ?: "patient.device@smaran.local"
    )
    val targetPatientEmail = _targetPatientEmail.asStateFlow()

    fun setTargetPatientEmail(email: String) {
        val clean = email.trim().lowercase()
        if (clean.isNotEmpty()) {
            _targetPatientEmail.value = clean
            prefs.edit().putString("target_patient_email", clean).apply()
            relay.subscribeForEmail(clean)
            refreshPatientData()
        }
    }

    // Initial safe zone (anchors to tracker's location once first ping arrives or custom placed)
    private val _zone = MutableStateFlow(
        GeofenceZone(
            id = UUID.randomUUID().toString().take(8),
            name = "My Safe Zone",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 300.0
        )
    )
    val zone = _zone.asStateFlow()

    private val _targetPing = MutableStateFlow<LocationPing?>(null)
    val targetPing = _targetPing.asStateFlow()

    private val _latestAlert = MutableStateFlow<BreachAlert?>(null)
    val latestAlert = _latestAlert.asStateFlow()

    private val _isBreached = MutableStateFlow(false)
    val isBreached = _isBreached.asStateFlow()

    private val _broadcastSuccess = MutableStateFlow(false)
    val broadcastSuccess = _broadcastSuccess.asStateFlow()

    private val _recenterTrigger = MutableStateFlow(0)
    val recenterTrigger = _recenterTrigger.asStateFlow()

    private val _isPlayingSound = MutableStateFlow(false)
    val isPlayingSound = _isPlayingSound.asStateFlow()

    private val _patientTelemetry = MutableStateFlow<PatientCognitiveTelemetry?>(loadCachedTelemetry())
    val patientTelemetry = _patientTelemetry.asStateFlow()

    private var breachAlertJob: Job? = null
    private var hasCustomZoneLocation = false

    private fun updateBreachStatus(dist: Double) {
        val z = _zone.value
        val breached = if (z.latitude != 0.0) dist > z.radiusMeters else false
        val wasBreached = _isBreached.value
        _isBreached.value = breached

        if (breached) {
            if (!wasBreached || breachAlertJob == null || !breachAlertJob!!.isActive) {
                startRepeatingBreachAlerts()
            }
        } else {
            // Target is back inside radius! Immediately clear breach, sound, and notifications
            stopRepeatingBreachAlerts()
        }
    }

    private fun startRepeatingBreachAlerts() {
        breachAlertJob?.cancel()
        breachAlertJob = viewModelScope.launch {
            val app = getApplication<Application>()
            while (_isBreached.value) {
                val ping = _targetPing.value
                val zoneName = _zone.value.name
                val dist = ping?.distanceFromCenter ?: 0.0
                val device = ping?.deviceName ?: "Tracked Device"

                NotificationHelper.showBreachNotification(app, zoneName, dist, device)
                SoundPlayer.playFindMySound(app)

                // Repeat notification popup & alarm every 15 seconds while phone is locked/off
                delay(15_000L)
            }
        }
    }

    private fun stopRepeatingBreachAlerts() {
        breachAlertJob?.cancel()
        breachAlertJob = null
        val app = getApplication<Application>()
        NotificationHelper.cancelBreachNotification(app)
        SoundPlayer.stopSound()
        _latestAlert.value = null
    }

    fun reconnect() {
        if (guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.connect(guardianEmail)
            }
        }
    }

    fun init(email: String) {
        guardianEmail = email.trim().lowercase()
        val target = _targetPatientEmail.value
        viewModelScope.launch {
            // Continuous watchdog: ensures relay reconnects automatically if network drops
            launch {
                while (isActive) {
                    if (guardianEmail.isNotEmpty() && (!relay.isConnected.value || !relay.isClientConnected)) {
                        relay.connect(guardianEmail)
                        relay.subscribeForEmail(guardianEmail)
                        if (target.isNotEmpty()) relay.subscribeForEmail(target)
                        refreshPatientData()
                    }
                    delay(3500L)
                }
            }

            // Initial connection and subscription
            if (guardianEmail.isNotEmpty()) {
                relay.connect(guardianEmail)
                relay.subscribeForEmail(guardianEmail)
                if (target.isNotEmpty()) relay.subscribeForEmail(target)
                delay(800L)
                refreshPatientData()
            }

            // Listen for active zone from broker
            launch {
                relay.activeZone.collect { existingZone ->
                    if (existingZone != null && existingZone.latitude != 0.0) {
                        hasCustomZoneLocation = true
                        _zone.value = existingZone
                    }
                }
            }

            // Listen for live location pings from remote target device
            launch {
                relay.latestPing.collect { ping ->
                    if (ping.latitude == 0.0 && ping.longitude == 0.0) return@collect

                    val current = _targetPing.value
                    if (current != null && current.timestamp > 0 && ping.timestamp > 0) {
                        // 1. Guard against stale retained broker messages delivered out-of-order
                        if (ping.timestamp < current.timestamp - 10_000L) {
                            android.util.Log.d("GuardianViewModel", "Discarded stale ping: ${ping.timestamp} vs current ${current.timestamp}")
                            return@collect
                        }

                        // 2. Multi-device conflict resolution: if current device is active (<60s), ignore older different devices
                        val currentAge = System.currentTimeMillis() - current.timestamp
                        val isCurrentDeviceActive = currentAge < 60_000L
                        if (isCurrentDeviceActive && current.deviceId.isNotBlank() && ping.deviceId != current.deviceId && ping.timestamp < current.timestamp) {
                            android.util.Log.d("GuardianViewModel", "Discarded competing ping from inactive device ${ping.deviceId}")
                            return@collect
                        }

                        // 3. Accuracy guard: do not overwrite high-precision GPS (<40m) with coarse cell-tower (>150m) if recent
                        if (currentAge < 90_000L && current.accuracy in 0.1f..40f && ping.accuracy > 150f) {
                            android.util.Log.d("GuardianViewModel", "Preserved high-accuracy GPS fix (${current.accuracy}m) over coarse fix (${ping.accuracy}m)")
                            return@collect
                        }
                    }

                    var z = _zone.value
                    // If safe zone center not set yet, anchor it to the tracker's initial position
                    if (!hasCustomZoneLocation && (z.latitude == 0.0 || z.longitude == 0.0) && ping.latitude != 0.0) {
                        z = z.copy(latitude = ping.latitude, longitude = ping.longitude)
                        _zone.value = z
                    }

                    val dist = if (z.latitude != 0.0 && ping.latitude != 0.0) {
                        LocationUtils.distanceMeters(ping.latitude, ping.longitude, z.latitude, z.longitude)
                    } else 0.0

                    updateBreachStatus(dist)
                    _targetPing.value = ping.copy(
                        distanceFromCenter = dist,
                        isBreach = _isBreached.value
                    )
                    if (ping.deviceName.isNotBlank() && _targetPatientEmail.value == "patient.device@smaran.local") {
                        val autoEmail = "patient.${ping.deviceName.replace(' ', '_').lowercase()}@smaran.local"
                        relay.subscribeForEmail(autoEmail)
                    }
                }
            }

            // Listen for breach alerts
            launch {
                relay.breachAlert.collect { alert ->
                    _latestAlert.value = alert
                    if (alert.status == "RESOLVED_INSIDE") {
                        updateBreachStatus(0.0)
                    } else {
                        val ping = _targetPing.value
                        val dist = ping?.distanceFromCenter ?: (alert.distanceMeters)
                        updateBreachStatus(dist)
                    }
                }
            }

            // Listen for live patient cognitive scores & games played telemetry
            launch {
                relay.latestTelemetry.collect { telemetry ->
                    if (telemetry != null) {
                        _patientTelemetry.value = telemetry
                        try {
                            prefs.edit().putString("cached_patient_telemetry", json.encodeToString(telemetry)).commit()
                        } catch (e: Exception) {
                            android.util.Log.e("GuardianViewModel", "Failed to cache telemetry: ${e.message}")
                        }
                        if (telemetry.patientEmail.isNotBlank() && (_targetPatientEmail.value == "patient.device@smaran.local" || _targetPatientEmail.value.isBlank())) {
                            _targetPatientEmail.value = telemetry.patientEmail
                            prefs.edit().putString("target_patient_email", telemetry.patientEmail).apply()
                            relay.subscribeForEmail(telemetry.patientEmail)
                        }
                    }
                }
            }
        }
    }

    fun refreshPatientData() {
        val target = _targetPatientEmail.value
        viewModelScope.launch {
            if (guardianEmail.isNotEmpty()) {
                relay.sendCommand(guardianEmail, "all", RemoteCommand(command = "FETCH_COGNITIVE_DATA"))
            }
            if (target.isNotEmpty() && target != guardianEmail) {
                relay.sendCommand(target, "all", RemoteCommand(command = "FETCH_COGNITIVE_DATA"))
            }
            relay.sendCommand("patient.device@smaran.local", "all", RemoteCommand(command = "FETCH_COGNITIVE_DATA"))
            relay.sendCommand("smaran_shared", "all", RemoteCommand(command = "FETCH_COGNITIVE_DATA"))
        }
    }

    fun updateCenter(lat: Double, lon: Double) {
        hasCustomZoneLocation = true
        _zone.value = _zone.value.copy(
            latitude = lat,
            longitude = lon,
            updatedAt = System.currentTimeMillis()
        )
        _targetPing.value?.let { ping ->
            val dist = LocationUtils.distanceMeters(ping.latitude, ping.longitude, lat, lon)
            updateBreachStatus(dist)
            _targetPing.value = ping.copy(
                distanceFromCenter = dist,
                isBreach = _isBreached.value
            )
        }
        broadcastZone()
    }

    fun updateRadius(radius: Double) {
        _zone.value = _zone.value.copy(
            radiusMeters = radius,
            updatedAt = System.currentTimeMillis()
        )
        _targetPing.value?.let { ping ->
            val dist = LocationUtils.distanceMeters(ping.latitude, ping.longitude, _zone.value.latitude, _zone.value.longitude)
            updateBreachStatus(dist)
            _targetPing.value = ping.copy(
                distanceFromCenter = dist,
                isBreach = _isBreached.value
            )
        }
        broadcastZone()
    }

    fun updateName(name: String) {
        _zone.value = _zone.value.copy(
            name = name,
            updatedAt = System.currentTimeMillis()
        )
        broadcastZone()
    }

    fun broadcastZone() {
        if (guardianEmail.isBlank()) return
        viewModelScope.launch {
            val success = relay.publishZone(guardianEmail, _zone.value)
            _broadcastSuccess.value = success
        }
    }

    fun triggerRecenter() {
        _recenterTrigger.value += 1
    }

    fun playSound() {
        _isPlayingSound.value = true
        SoundPlayer.playFindMySound(getApplication())
        val deviceId = _targetPing.value?.deviceId
        if (!deviceId.isNullOrEmpty() && guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.sendCommand(
                    targetEmail = guardianEmail,
                    deviceId = deviceId,
                    command = RemoteCommand("PLAY_SOUND", guardianEmail)
                )
            }
        }
    }

    fun stopSound() {
        _isPlayingSound.value = false
        SoundPlayer.stopSound()
        val deviceId = _targetPing.value?.deviceId
        if (!deviceId.isNullOrEmpty() && guardianEmail.isNotEmpty()) {
            viewModelScope.launch {
                relay.sendCommand(
                    targetEmail = guardianEmail,
                    deviceId = deviceId,
                    command = RemoteCommand("STOP_SOUND", guardianEmail)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRepeatingBreachAlerts()
    }
}
