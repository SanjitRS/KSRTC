package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProfile(
    val deviceId: String = "",
    val deviceName: String = "Android Device",
    val ownerEmail: String = "",
    val authorizedGoogleEmails: List<String> = emptyList(),
    val isSharingActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
