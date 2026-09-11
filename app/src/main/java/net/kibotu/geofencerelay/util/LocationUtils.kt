package net.kibotu.geofencerelay.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kibotu.geofencerelay.model.GeofenceZone
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object LocationUtils {

    data class RealLocation(
        val latitude: Double,
        val longitude: Double,
        val city: String = "",
        val country: String = ""
    )

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("google_sdk"))
    }

    fun isGoogleplexDefault(lat: Double, lon: Double): Boolean {
        return Math.abs(lat - 37.422) < 0.05 && Math.abs(lon - (-122.084)) < 0.05
    }

    suspend fun fetchIpLocation(): RealLocation? = withContext(Dispatchers.IO) {
        // Try freeipapi.com first (HTTPS)
        try {
            val url = URL("https://freeipapi.com/api/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 GeofenceRelay")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val lat = json.optDouble("latitude", 0.0)
                val lon = json.optDouble("longitude", 0.0)
                val city = json.optString("cityName", "")
                val country = json.optString("countryName", "")
                if (lat != 0.0 && lon != 0.0) {
                    return@withContext RealLocation(lat, lon, city, country)
                }
            }
        } catch (_: Exception) {}

        // Fallback to ip-api.com (HTTP)
        try {
            val url = URL("http://ip-api.com/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "GeofenceRelay")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                if (json.optString("status") == "success") {
                    val lat = json.optDouble("lat", 0.0)
                    val lon = json.optDouble("lon", 0.0)
                    val city = json.optString("city", "")
                    val country = json.optString("country", "")
                    if (lat != 0.0 && lon != 0.0) {
                        return@withContext RealLocation(lat, lon, city, country)
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    fun getFriendlyDeviceName(rawModel: String?): String {
        if (rawModel.isNullOrBlank()) return "My Phone"
        return when {
            rawModel.contains("sdk_gphone", ignoreCase = true) || rawModel.contains("emulator", ignoreCase = true) -> "My Device (Pixel 8)"
            else -> rawModel
        }
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    fun isInsideZone(latitude: Double, longitude: Double, zone: GeofenceZone?): Boolean {
        if (zone == null) return true
        val distance = distanceMeters(latitude, longitude, zone.latitude, zone.longitude)
        return distance <= zone.radiusMeters
    }

    suspend fun getReadableAddress(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) return@withContext "Unknown location"
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var addressResult = "Near current position"
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr: Address = addresses[0]
                    val feature = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: addr.featureName
                    val city = addr.locality ?: addr.adminArea ?: ""
                    addressResult = if (!feature.isNullOrBlank()) "$feature, $city".trim().removeSuffix(",") else city
                }
                addressResult
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: addr.featureName
                    val city = addr.locality ?: addr.adminArea ?: ""
                    if (!feature.isNullOrBlank()) "$feature, $city".trim().removeSuffix(",") else city
                } else {
                    "Near current position"
                }
            }
        } catch (_: Exception) {
            "%.4f, %.4f".format(Locale.US, latitude, longitude)
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.2f km", meters / 1000)
        } else {
            String.format(Locale.getDefault(), "%d m", meters.roundToInt())
        }
    }

    fun formatSpeed(speedMps: Float): String {
        val kmh = speedMps * 3.6f
        return String.format(Locale.getDefault(), "%.1f km/h", kmh)
    }

    fun formatTime(timestamp: Long): String {
        val diffSeconds = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            diffSeconds < 10 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            else -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
