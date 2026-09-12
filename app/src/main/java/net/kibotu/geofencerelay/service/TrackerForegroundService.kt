package net.kibotu.geofencerelay.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.kibotu.geofencerelay.model.BreachAlert
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.relay.CognitiveTelemetryManager
import net.kibotu.geofencerelay.relay.MqttRelayClient
import net.kibotu.geofencerelay.util.BatteryUtils
import net.kibotu.geofencerelay.util.LocationUtils
import net.kibotu.geofencerelay.util.NotificationHelper
import net.kibotu.geofencerelay.util.SoundPlayer
import java.util.UUID

class TrackerForegroundService : Service() {

    private val tag = "FindMyService"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var deviceId: String = ""
    private var deviceName: String = ""
    private var activeZone: GeofenceZone? = null
    private var isCurrentlyBreached: Boolean = false

    private var lastKnownLocation: Location? = null
    private var lastBroadcastTimestamp: Long = 0L

    // Non-blocking reverse geocoding cache
    private var lastKnownAddress: String = "Locating nearby area..."
    private var lastGeocodedLat: Double = 0.0
    private var lastGeocodedLon: Double = 0.0
    private var geocodeJob: Job? = null

    private var listenersJob: Job? = null
    private var loopJob: Job? = null
    private var lastCognitiveBroadcastTime: Long = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    private val systemLocationListener = LocationListener { location ->
        handleNewLocation(location)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().take(8).also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
        deviceName = LocationUtils.getFriendlyDeviceName(Build.MODEL)

        // Immediately establish foreground state to prevent ForegroundServiceDidNotStartInTimeException
        startForegroundNotification()
        _serviceRunning.value = true
    }

    private fun startForegroundNotification() {
        val initialNotification = NotificationHelper.buildServiceNotification(
            context = this,
            isBreached = false,
            statusText = "Sharing location with authorized Google Accounts..."
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.SERVICE_NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, initialNotification)
            }
        } catch (e: SecurityException) {
            Log.w(tag, "startForeground location type rejected (${e.message}), attempting fallback")
            try {
                startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, initialNotification)
            } catch (ex: Exception) {
                Log.e(tag, "Fallback startForeground failed: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(tag, "startForeground error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        startTracking()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_RUNNING, true).apply()
        _serviceRunning.value = true

        // 1. Connect and sync authorized accounts immediately
        syncAuthorizedEmailsAndBroadcast()

        // 2. Fetch IMMEDIATE Location fix using all available hardware providers
        fetchImmediateLocationFix()

        // 3. Request continuous location updates
        requestLocationUpdates(isHighFrequency = false)

        // 4. Immediately broadcast latest cognitive score to Caregiver
        CognitiveTelemetryManager.broadcastLatest(applicationContext)
    }

    /**
     * Determines whether one Location reading is better than the current Location fix.
     * Prevents coarse network/cell tower fixes from overriding fresh high-accuracy GPS fixes,
     * but always accepts locations if more than 15 seconds have elapsed or if current fix is missing.
     */
    private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            return true
        }

        val timeDelta: Long = location.time - currentBestLocation.time
        val isSignificantlyNewer: Boolean = timeDelta > 15_000L
        val isSignificantlyOlder: Boolean = timeDelta < -60_000L
        val isNewer: Boolean = timeDelta > 0

        // If it's been more than 15 seconds, accept new location as user may have moved
        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        val accuracyDelta: Float = location.accuracy - currentBestLocation.accuracy
        val isLessAccurate: Boolean = accuracyDelta > 0
        val isMoreAccurate: Boolean = accuracyDelta < 0
        val isSignificantlyLessAccurate: Boolean = accuracyDelta > 100f

        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun fetchImmediateLocationFix() {
        // A. Check Android system LocationManager (GPS, Network, Passive)
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    if (lm.isProviderEnabled(provider)) {
                        val loc = lm.getLastKnownLocation(provider)
                        if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                            handleNewLocation(loc)
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // B. Check Google Play Services FusedLocationProviderClient lastLocation
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    handleNewLocation(loc)
                }
            }
        } catch (_: Exception) {}

        // C. Request fresh location fix: High Accuracy with indoor Balanced Power fallback
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null && loc.latitude != 0.0) {
                        handleNewLocation(loc)
                    } else {
                        try {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                                .addOnSuccessListener { fallbackLoc ->
                                    if (fallbackLoc != null && fallbackLoc.latitude != 0.0) {
                                        handleNewLocation(fallbackLoc)
                                    }
                                }
                        } catch (_: Exception) {}
                    }
                }
        } catch (_: Exception) {}
    }

    fun syncAuthorizedEmailsAndBroadcast() {
        val authorizedEmails = getAuthorizedEmails()

        // Immediate connection & subscription
        scope.launch {
            if (authorizedEmails.isNotEmpty()) {
                val primary = authorizedEmails.first()
                if (!MqttRelayClient.shared.isConnected.value || !MqttRelayClient.shared.isClientConnected) {
                    MqttRelayClient.shared.connect(primary)
                }
                for (email in authorizedEmails) {
                    MqttRelayClient.shared.subscribeForEmail(email)
                }
            }
            CognitiveTelemetryManager.broadcastLatest(applicationContext)
            fetchImmediateLocationFix()
        }

        // Setup persistent event listeners once
        if (listenersJob == null || listenersJob?.isActive != true) {
            listenersJob = scope.launch {
                // Remote commands (e.g. Find My "Play Sound")
                launch {
                    MqttRelayClient.shared.incomingCommand.collect { cmd ->
                        when {
                            cmd.command == "PLAY_SOUND" -> SoundPlayer.playFindMySound(applicationContext)
                            cmd.command == "STOP_SOUND" -> SoundPlayer.stopSound()
                            cmd.command.startsWith("SET_PATIENT_AGE:") -> {
                                val age = cmd.command.removePrefix("SET_PATIENT_AGE:").trim().toIntOrNull()
                                if (age != null && age in 18..110) {
                                    Log.i(tag, "Received command to set patient biological age to $age")
                                    CognitiveTelemetryManager.setBiologicalAge(applicationContext, age)
                                    CognitiveTelemetryManager.broadcastLatest(applicationContext)
                                }
                            }
                            cmd.command.contains("COGNITIVE", ignoreCase = true) || cmd.command.contains("SYNC", ignoreCase = true) -> {
                                Log.i(tag, "Received request to sync cognitive telemetry")
                                CognitiveTelemetryManager.broadcastLatest(applicationContext)
                            }
                        }
                    }
                }

                // Active safe zone updates from Guardian
                launch {
                    MqttRelayClient.shared.activeZone.collect { zone ->
                        if (zone != null) {
                            activeZone = zone
                            Log.d(tag, "Active safe zone updated: ${zone.name}, r=${zone.radiusMeters}m")
                            updateNotification("Monitoring Safe Zone: ${zone.name}")
                        }
                    }
                }
            }
        }

        // Setup periodic telemetry heartbeat loop
        if (loopJob == null || loopJob?.isActive != true) {
            loopJob = scope.launch {
                while (isActive) {
                    try {
                        val emails = getAuthorizedEmails()
                        if (emails.isNotEmpty()) {
                            if (!MqttRelayClient.shared.isConnected.value || !MqttRelayClient.shared.isClientConnected) {
                                MqttRelayClient.shared.connect(emails.first())
                            }
                            for (email in emails) {
                                MqttRelayClient.shared.subscribeForEmail(email)
                            }
                        }

                        // Poll for fresh location fix
                        fetchImmediateLocationFix()

                        // Stationary Heartbeat: If device is resting still indoors, heartbeat last fix every 3s
                        broadcastHeartbeatIfStationary()

                        // Periodically refresh and broadcast latest cognitive telemetry (every 15s)
                        val now = System.currentTimeMillis()
                        if (now - lastCognitiveBroadcastTime > 15_000L) {
                            lastCognitiveBroadcastTime = now
                            CognitiveTelemetryManager.broadcastLatest(applicationContext)
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Periodic telemetry loop error: ${e.message}")
                    }
                    delay(3000L)
                }
            }
        }
    }

    private fun broadcastHeartbeatIfStationary() {
        val now = System.currentTimeMillis()
        if (now - lastBroadcastTimestamp > 3200L) {
            val loc = lastKnownLocation
            if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                broadcastLocationPing(loc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(isHighFrequency: Boolean) {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        val intervalMs = if (isHighFrequency) 1500L else 3000L

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        )
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.w(tag, "FusedLocationProviderClient updates request failed: ${e.message}")
        }

        // Hardware LocationManager listener fallback (GPS + Network indoor/outdoor updates)
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, 0f, systemLocationListener, Looper.getMainLooper())
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, intervalMs, 0f, systemLocationListener, Looper.getMainLooper())
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleNewLocation(location: Location) {
        val cur = lastKnownLocation
        if (cur != null && !isBetterLocation(location, cur)) {
            Log.d(tag, "Ignored coarse/stale location (${location.provider} acc=${location.accuracy}m vs current acc=${cur.accuracy}m)")
            return
        }

        lastKnownLocation = Location(location)
        broadcastLocationPing(location)
    }

    private fun broadcastLocationPing(location: Location) {
        scope.launch {
            try {
                var lat = location.latitude
                var lon = location.longitude

                if (lat == 0.0 && lon == 0.0) return@launch

                // If emulator default California coordinate, fetch real physical IP location
                if (LocationUtils.isEmulator() && LocationUtils.isGoogleplexDefault(lat, lon)) {
                    val realLoc = LocationUtils.fetchIpLocation()
                    if (realLoc != null) {
                        lat = realLoc.latitude
                        lon = realLoc.longitude
                    }
                }

                // Save last known coordinates
                val currentLoc = Location(location).apply {
                    latitude = lat
                    longitude = lon
                }
                lastKnownLocation = currentLoc

                val zone = activeZone
                val accuracy = location.accuracy
                val speed = location.speed

                val distance = if (zone != null && zone.latitude != 0.0) {
                    LocationUtils.distanceMeters(lat, lon, zone.latitude, zone.longitude)
                } else {
                    0.0
                }

                val isInside = if (zone != null && zone.latitude != 0.0) distance <= zone.radiusMeters else true

                // Breach transition check
                if (!isInside && !isCurrentlyBreached) {
                    isCurrentlyBreached = true
                    Log.w(tag, "BREACH: Outside safe zone by ${distance}m")

                    NotificationHelper.showBreachNotification(
                        this@TrackerForegroundService,
                        zone?.name ?: "Safe Zone",
                        distance - (zone?.radiusMeters ?: 0.0)
                    )
                    updateNotification("âš ï¸ OUTSIDE SAFE ZONE (${LocationUtils.formatDistance(distance)} from center)")
                    requestLocationUpdates(isHighFrequency = true)

                    val alert = BreachAlert(
                        deviceId = deviceId,
                        geofenceId = zone?.id ?: "",
                        geofenceName = zone?.name ?: "Safe Zone",
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = distance,
                        status = "BREACH_STARTED"
                    )
                    broadcastToAuthorizedAccounts { email ->
                        MqttRelayClient.shared.publishAlert(email, alert)
                    }
                } else if (isInside && isCurrentlyBreached) {
                    isCurrentlyBreached = false
                    NotificationHelper.cancelBreachNotification(this@TrackerForegroundService)
                    updateNotification("ðŸŸ¢ Back inside ${zone?.name ?: "Safe Zone"}")
                    requestLocationUpdates(isHighFrequency = false)

                    val alert = BreachAlert(
                        deviceId = deviceId,
                        geofenceId = zone?.id ?: "",
                        geofenceName = zone?.name ?: "Safe Zone",
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = distance,
                        status = "RESOLVED_INSIDE"
                    )
                    broadcastToAuthorizedAccounts { email ->
                        MqttRelayClient.shared.publishAlert(email, alert)
                    }
                }

                // Asynchronous, non-blocking reverse geocoding
                val distSinceGeocode = if (lastGeocodedLat != 0.0) {
                    LocationUtils.distanceMeters(lat, lon, lastGeocodedLat, lastGeocodedLon)
                } else Double.MAX_VALUE

                if (distSinceGeocode > 50.0 || lastKnownAddress == "Locating nearby area...") {
                    lastGeocodedLat = lat
                    lastGeocodedLon = lon
                    geocodeJob?.cancel()
                    geocodeJob = scope.launch(Dispatchers.IO) {
                        val resolved = LocationUtils.getReadableAddress(applicationContext, lat, lon)
                        if (resolved.isNotBlank()) {
                            lastKnownAddress = resolved
                        }
                    }
                }

                val myEmail = applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("user_google_email", "") ?: ""
                val currentBioAge = CognitiveTelemetryManager.getBiologicalAge(applicationContext)
                val latestTel = CognitiveTelemetryManager.getLatestTelemetry(applicationContext, myEmail)
                val currentCps = latestTel?.compositeCps ?: 85.0
                val currentCogAge = latestTel?.functionalCognitiveAge ?: (currentBioAge - 2).toDouble().coerceAtLeast(18.0)
                val currentTrajectory = latestTel?.trajectoryStatus ?: "STABLE"

                // Live telemetry broadcast with battery and cached address
                val battery = BatteryUtils.getBatteryStatus(applicationContext)
                val ping = LocationPing(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    patientEmail = myEmail,
                    biologicalAge = currentBioAge,
                    compositeCps = currentCps,
                    functionalCognitiveAge = currentCogAge,
                    trajectoryStatus = currentTrajectory,
                    latitude = lat,
                    longitude = lon,
                    accuracy = accuracy,
                    speed = speed,
                    batteryLevel = battery.level,
                    isCharging = battery.isCharging,
                    address = lastKnownAddress,
                    isBreach = isCurrentlyBreached,
                    distanceFromCenter = distance,
                    timestamp = System.currentTimeMillis()
                )

                broadcastToAuthorizedAccounts { email ->
                    MqttRelayClient.shared.publishPing(email, ping)
                }
                _latestDevicePing.value = ping
                lastBroadcastTimestamp = System.currentTimeMillis()
                Log.d(tag, "Broadcast ping from $deviceName to ${getAuthorizedEmails().size} accounts ($lat, $lon)")
            } catch (e: Exception) {
                Log.e(tag, "Error handling new location: ${e.message}", e)
            }
        }
    }

    private suspend fun broadcastToAuthorizedAccounts(action: suspend (String) -> Unit) {
        val emails = getAuthorizedEmails().toMutableSet()
        emails.add("patient.device@smaran.local")
        emails.add("guardian.device@smaran.local")
        emails.add("smaran_shared")
        for (email in emails) {
            if (email.isBlank()) continue
            try {
                if (!MqttRelayClient.shared.isConnected.value) {
                    MqttRelayClient.shared.connect(email)
                }
                action(email)
            } catch (e: Exception) {
                Log.e(tag, "Failed to broadcast to $email: ${e.message}")
            }
        }
    }

    private fun getAuthorizedEmails(): Set<String> {
        return getAuthorizedEmails(this)
    }

    private fun updateNotification(statusText: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationHelper.buildServiceNotification(
                context = this,
                isBreached = isCurrentlyBreached,
                statusText = statusText
            )
            nm.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun stopTracking() {
        listenersJob?.cancel()
        listenersJob = null
        loopJob?.cancel()
        loopJob = null
        geocodeJob?.cancel()
        geocodeJob = null

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.removeUpdates(systemLocationListener)
        } catch (_: Exception) {}
        try {
            SoundPlayer.stopSound()
        } catch (_: Exception) {}
        try {
            MqttRelayClient.shared.disconnect()
        } catch (_: Exception) {}
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_RUNNING, false).apply()
        _serviceRunning.value = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopTracking()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private var instance: TrackerForegroundService? = null

        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"

        const val PREFS_NAME = "findmy_tracker_prefs"
        const val KEY_IS_RUNNING = "key_is_running"
        const val KEY_DEVICE_ID = "key_device_id"
        const val KEY_AUTHORIZED_EMAILS = "key_authorized_emails"

        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning = _serviceRunning.asStateFlow()

        private val _latestDevicePing = MutableStateFlow<LocationPing?>(null)
        val latestDevicePing = _latestDevicePing.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, TrackerForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("FindMyService", "Failed to start TrackerForegroundService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("FindMyService", "Failed to stop TrackerForegroundService: ${e.message}", e)
            }
        }

        fun isRunning(context: Context): Boolean {
            return _serviceRunning.value || context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_RUNNING, false)
        }

        fun notifyAuthorizedEmailsChanged(context: Context) {
            instance?.syncAuthorizedEmailsAndBroadcast()
        }

        fun addAuthorizedEmail(context: Context, email: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_AUTHORIZED_EMAILS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(email.trim().lowercase())
            prefs.edit().putStringSet(KEY_AUTHORIZED_EMAILS, current).apply()
            notifyAuthorizedEmailsChanged(context)
        }

        fun removeAuthorizedEmail(context: Context, email: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_AUTHORIZED_EMAILS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.remove(email.trim().lowercase())
            prefs.edit().putStringSet(KEY_AUTHORIZED_EMAILS, current).apply()
            notifyAuthorizedEmailsChanged(context)
        }

        fun getAuthorizedEmails(context: Context): Set<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val loggedInEmail = authPrefs.getString("user_google_email", null)?.trim()?.lowercase()

            val saved = prefs.getStringSet(KEY_AUTHORIZED_EMAILS, null)
            val result = (saved ?: emptySet()).map { it.trim().lowercase() }.toMutableSet()
            if (!loggedInEmail.isNullOrBlank()) {
                result.add(loggedInEmail)
            }
            if (result.isEmpty()) {
                result.add("patient.device@smaran.local")
            }
            return result
        }
    }
}