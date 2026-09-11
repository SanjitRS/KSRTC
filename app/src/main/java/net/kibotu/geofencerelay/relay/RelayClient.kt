package net.kibotu.geofencerelay.relay

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.kibotu.geofencerelay.model.BreachAlert
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.model.RemoteCommand

interface RelayClient {
    val isConnected: StateFlow<Boolean>
    val activeZone: StateFlow<GeofenceZone?>
    val latestPing: SharedFlow<LocationPing>
    val breachAlert: SharedFlow<BreachAlert>
    val incomingCommand: SharedFlow<RemoteCommand>

    suspend fun connect(userEmail: String): Boolean
    suspend fun publishZone(targetEmail: String, zone: GeofenceZone): Boolean
    suspend fun publishPing(targetEmail: String, ping: LocationPing): Boolean
    suspend fun publishAlert(targetEmail: String, alert: BreachAlert): Boolean
    suspend fun sendCommand(targetEmail: String, deviceId: String, command: RemoteCommand): Boolean
    fun disconnect()
}
