package net.kibotu.geofencerelay.ui.navigation

sealed class AppScreen {
    object RoleSelect : AppScreen()
    data class Auth(val isTracker: Boolean) : AppScreen()
    data class Tracker(val email: String) : AppScreen()
    data class Guardian(val email: String) : AppScreen()
}
