package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class BreachAlert(
    val deviceId: String = "",
    val geofenceId: String = "",
    val geofenceName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "BREACH_STARTED" // "BREACH_STARTED", "BREACH_ONGOING", "RESOLVED_INSIDE"
)
