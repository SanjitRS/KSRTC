package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteCommand(
    val command: String = "PLAY_SOUND", // "PLAY_SOUND", "STOP_SOUND"
    val senderGoogleEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
