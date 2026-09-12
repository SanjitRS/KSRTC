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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun loadCachedTelemetry(): PatientCognitiveTelemetry? {
        val raw = prefs.getString("cached_patient_telemetry", null) ?: return null
        return try {
            json.decodeFromString<PatientCognitiveTelemetry>(raw)
        } catch (t: Throwable) {
            android.util.Log.e("GuardianViewModel", "Error reading cached telemetry: ${t.message}")
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
                        val cur = _zone.value
                        if (cur.latitude == 0.0 || existingZone.updatedAt >= cur.updatedAt) {
                            hasCustomZoneLocation = true
                            _zone.value = existingZone
                        }
                    }
                }
            }

            // Listen for live location pings from remote target device
            launch {
                relay.latestPing.collect { ping ->
                    if (ping.latitude == 0.0 && ping.longitude == 0.0) return@collect

                    val target = _targetPatientEmail.value.trim().lowercase()
                    val pingEmail = ping.patientEmail.trim().lowercase()

                    // Strict Target Filtering:
                    // If target is configured, ONLY process pings from that patient!
                    if (target.isNotEmpty() && target != "patient.device@smaran.local") {
                        if (pingEmail.isNotEmpty() && pingEmail != target) {
                            return@collect
                        }
                    } else {
                        // If target was empty/default and we receive a real email, lock onto it
                        if (pingEmail.isNotEmpty() && pingEmail != "patient.device@smaran.local") {
                            _targetPatientEmail.value = pingEmail
                            prefs.edit().putString("target_patient_email", pingEmail).apply()
                            relay.subscribeForEmail(pingEmail)
                        }
                    }

                    val currentPing = _targetPing.value
                    // Discard stale packets from offline devices (> 2 minutes old)
                    if (currentPing != null && ping.timestamp > 0 && ping.timestamp < currentPing.timestamp - 120_000L) {
                        return@collect
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

                    // Real-time live CPS telemetry sync embedded in 3s location ping
                    if (ping.compositeCps > 0.0 || ping.recentGameSessionsJson.isNotBlank() || ping.totalGamesPlayedToday > 0) {
                        val current = _patientTelemetry.value
                        val decodedSessions: List<GameSessionRecord> = if (ping.recentGameSessionsJson.isNotBlank()) {
                            try {
                                json.decodeFromString<List<GameSessionRecord>>(ping.recentGameSessionsJson)
                            } catch (_: Throwable) {
                                current?.recentGameSessions ?: emptyList()
                            }
                        } else {
                            current?.recentGameSessions ?: emptyList()
                        }

                        val gamesCount = if (ping.totalGamesPlayedToday > 0) {
                            ping.totalGamesPlayedToday
                        } else if (decodedSessions.isNotEmpty()) {
                            decodedSessions.size
                        } else {
                            current?.totalGamesPlayedToday ?: 0
                        }

                        val newAge = if (ping.biologicalAge > 0) ping.biologicalAge else (current?.biologicalAge ?: 68)
                        val newCogAge = if (ping.functionalCognitiveAge > 0.0) ping.functionalCognitiveAge else (current?.functionalCognitiveAge ?: (newAge - 2).toDouble().coerceAtLeast(18.0))
                        val newTrajectory = ping.trajectoryStatus.ifBlank { current?.trajectoryStatus ?: "STABLE" }

                        val newTel = (current ?: PatientCognitiveTelemetry()).copy(
                            patientEmail = ping.patientEmail.ifBlank { current?.patientEmail ?: "" },
                            compositeCps = if (ping.compositeCps > 0.0) ping.compositeCps else (current?.compositeCps ?: 85.0),
                            functionalCognitiveAge = newCogAge,
                            biologicalAge = newAge,
                            memoryRetentionIndex = if (ping.memoryRetentionIndex > 0.0) ping.memoryRetentionIndex else (current?.memoryRetentionIndex ?: 82.0),
                            executiveFunctionIndex = if (ping.executiveFunctionIndex > 0.0) ping.executiveFunctionIndex else (current?.executiveFunctionIndex ?: 88.0),
                            reactionLatencyScore = if (ping.reactionLatencyScore > 0.0) ping.reactionLatencyScore else (current?.reactionLatencyScore ?: 85.0),
                            errorRecoveryRate = if (ping.errorRecoveryRate > 0.0) ping.errorRecoveryRate else (current?.errorRecoveryRate ?: 80.0),
                            circadianRisk = ping.circadianRisk.ifBlank { current?.circadianRisk ?: "Low" },
                            fatigueIndex = if (ping.fatigueIndex > 0.0) ping.fatigueIndex else (current?.fatigueIndex ?: 0.15),
                            trajectoryStatus = newTrajectory,
                            totalGamesPlayedToday = gamesCount,
                            recentGameSessions = if (decodedSessions.isNotEmpty()) decodedSessions else (current?.recentGameSessions ?: emptyList()),
                            timestamp = ping.timestamp
                        )
                        _patientTelemetry.value = newTel
                        try {
                            prefs.edit().putString("cached_patient_telemetry", json.encodeToString(newTel)).commit()
                        } catch (_: Exception) {}
                    }

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
                        val currentTarget = _targetPatientEmail.value.trim().lowercase()
                        val telEmail = telemetry.patientEmail.trim().lowercase()
                        val isConfigured = currentTarget.isNotEmpty() && currentTarget != "patient.device@smaran.local"
                        if (isConfigured && telEmail.isNotEmpty() && telEmail != currentTarget) {
                            // Discard telemetry from non-targeted accounts to prevent cross-device contamination
                            return@collect
                        }

                        val currentTel = _patientTelemetry.value
                        val mergedSessions = if (telemetry.recentGameSessions.isNotEmpty()) {
                            telemetry.recentGameSessions
                        } else {
                            currentTel?.recentGameSessions ?: emptyList()
                        }
                        val mergedGamesCount = if (telemetry.totalGamesPlayedToday > 0) {
                            telemetry.totalGamesPlayedToday
                        } else if (mergedSessions.isNotEmpty()) {
                            mergedSessions.size
                        } else {
                            currentTel?.totalGamesPlayedToday ?: 0
                        }
                        val mergedTel = telemetry.copy(
                            recentGameSessions = mergedSessions,
                            totalGamesPlayedToday = mergedGamesCount,
                            compositeCps = if (telemetry.compositeCps > 0.0) telemetry.compositeCps else (currentTel?.compositeCps ?: 85.0),
                            functionalCognitiveAge = if (telemetry.functionalCognitiveAge > 0.0) telemetry.functionalCognitiveAge else (currentTel?.functionalCognitiveAge ?: 66.0),
                            biologicalAge = if (telemetry.biologicalAge > 0) telemetry.biologicalAge else (currentTel?.biologicalAge ?: 68)
                        )
                        _patientTelemetry.value = mergedTel
                        try {
                            prefs.edit().putString("cached_patient_telemetry", json.encodeToString(mergedTel)).commit()
                        } catch (e: Exception) {
                            android.util.Log.e("GuardianViewModel", "Failed to cache telemetry: ${e.message}")
                        }
                        if (!isConfigured && telemetry.patientEmail.isNotBlank()) {
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
            relay.sendCommand("patient_device_at_smaran_local", "all", RemoteCommand(command = "FETCH_COGNITIVE_DATA"))
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
        val email = guardianEmail.ifBlank { "guardian.device@smaran.local" }
        val target = _targetPatientEmail.value
        viewModelScope.launch {
            val success = relay.publishZone(email, _zone.value)
            if (target.isNotBlank() && target != email) {
                relay.publishZone(target, _zone.value)
            }
            relay.publishZone("patient.device@smaran.local", _zone.value)
            relay.publishZone("smaran_shared", _zone.value)
            _broadcastSuccess.value = success
        }
    }

    fun triggerRecenter() {
        _recenterTrigger.value += 1
    }

    fun playSound() {
        _isPlayingSound.value = true
        SoundPlayer.playFindMySound(getApplication())
        val deviceId = _targetPing.value?.deviceId ?: "all"
        val email = guardianEmail.ifBlank { "guardian.device@smaran.local" }
        val target = _targetPatientEmail.value
        viewModelScope.launch {
            relay.sendCommand(
                targetEmail = email,
                deviceId = deviceId,
                command = RemoteCommand("PLAY_SOUND", email)
            )
            if (target.isNotBlank() && target != email) {
                relay.sendCommand(
                    targetEmail = target,
                    deviceId = deviceId,
                    command = RemoteCommand("PLAY_SOUND", email)
                )
            }
            relay.sendCommand("patient.device@smaran.local", deviceId, RemoteCommand("PLAY_SOUND", email))
            relay.sendCommand("smaran_shared", deviceId, RemoteCommand("PLAY_SOUND", email))
        }
    }

    fun stopSound() {
        _isPlayingSound.value = false
        SoundPlayer.stopSound()
        val deviceId = _targetPing.value?.deviceId ?: "all"
        val email = guardianEmail.ifBlank { "guardian.device@smaran.local" }
        val target = _targetPatientEmail.value
        viewModelScope.launch {
            relay.sendCommand(
                targetEmail = email,
                deviceId = deviceId,
                command = RemoteCommand("STOP_SOUND", email)
            )
            if (target.isNotBlank() && target != email) {
                relay.sendCommand(
                    targetEmail = target,
                    deviceId = deviceId,
                    command = RemoteCommand("STOP_SOUND", email)
                )
            }
            relay.sendCommand("patient.device@smaran.local", deviceId, RemoteCommand("STOP_SOUND", email))
            relay.sendCommand("smaran_shared", deviceId, RemoteCommand("STOP_SOUND", email))
        }
    }

    fun updatePatientAge(age: Int) {
        if (age !in 18..110) return
        val current = _patientTelemetry.value
        if (current != null) {
            val delta = age - current.biologicalAge
            val updated = current.copy(
                biologicalAge = age,
                functionalCognitiveAge = (current.functionalCognitiveAge + delta).coerceIn(18.0, 110.0),
                timestamp = System.currentTimeMillis()
            )
            _patientTelemetry.value = updated
            try {
                prefs.edit().putString("cached_patient_telemetry", json.encodeToString(updated)).commit()
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            val cmd = RemoteCommand(command = "SET_PATIENT_AGE:$age")
            if (guardianEmail.isNotEmpty()) relay.sendCommand(guardianEmail, "all", cmd)
            val target = _targetPatientEmail.value
            if (target.isNotEmpty() && target != guardianEmail) relay.sendCommand(target, "all", cmd)
            relay.sendCommand("patient.device@smaran.local", "all", cmd)
            relay.sendCommand("smaran_shared", "all", cmd)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRepeatingBreachAlerts()
    }
}
