package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class GeofenceZone(
    val id: String = "",
    val name: String = "Safe Zone",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Double = 200.0,
    val updatedAt: Long = System.currentTimeMillis()
)
