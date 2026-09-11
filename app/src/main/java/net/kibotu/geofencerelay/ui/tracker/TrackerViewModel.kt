package net.kibotu.geofencerelay.ui.tracker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.relay.MqttRelayClient
import net.kibotu.geofencerelay.service.TrackerForegroundService

class TrackerViewModel : ViewModel() {

    private val relay = MqttRelayClient.shared

    val isConnected: StateFlow<Boolean> = relay.isConnected
    val activeZone: StateFlow<GeofenceZone?> = relay.activeZone
    val isServiceRunning: StateFlow<Boolean> = TrackerForegroundService.serviceRunning
    val latestPing: StateFlow<LocationPing?> = TrackerForegroundService.latestDevicePing

    val isBreached: StateFlow<Boolean> = latestPing.map { ping ->
        ping?.isBreach ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _authorizedEmails = MutableStateFlow<Set<String>>(emptySet())
    val authorizedEmails = _authorizedEmails.asStateFlow()

    fun loadAuthorizedEmails(context: Context) {
        _authorizedEmails.value = TrackerForegroundService.getAuthorizedEmails(context)
    }

    fun addAuthorizedEmail(context: Context, email: String) {
        if (email.isNotBlank() && email.contains("@")) {
            TrackerForegroundService.addAuthorizedEmail(context, email)
            loadAuthorizedEmails(context)
            viewModelScope.launch {
                relay.subscribeForEmail(email)
            }
        }
    }

    fun removeAuthorizedEmail(context: Context, email: String) {
        TrackerForegroundService.removeAuthorizedEmail(context, email)
        loadAuthorizedEmails(context)
    }

    fun startTracking(context: Context) {
        TrackerForegroundService.start(context)
    }

    fun stopTracking(context: Context) {
        TrackerForegroundService.stop(context)
    }
}
