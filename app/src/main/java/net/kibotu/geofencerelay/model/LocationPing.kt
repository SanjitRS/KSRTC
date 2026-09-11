package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationPing(
    val deviceId: String = "",
    val deviceName: String = "Android Device",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val address: String = "Locating...",
    val timestamp: Long = System.currentTimeMillis(),
    val isBreach: Boolean = false,
    val distanceFromCenter: Double = 0.0
)
