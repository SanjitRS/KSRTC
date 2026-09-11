package net.kibotu.geofencerelay.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import net.kibotu.geofencerelay.BuildConfig
import net.kibotu.geofencerelay.ui.auth.GoogleSignInScreen
import net.kibotu.geofencerelay.ui.guardian.GuardianScreen
import net.kibotu.geofencerelay.ui.home.HomeScreen
import net.kibotu.geofencerelay.ui.navigation.AppScreen
import net.kibotu.geofencerelay.ui.theme.GeofenceRelayTheme
import net.kibotu.geofencerelay.features.ai.ui.SpringboardScreen
import net.kibotu.geofencerelay.ui.tracker.TrackerMainScreen
import net.kibotu.geofencerelay.util.NotificationHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannels(this)
        net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager.ensureAlarmScheduled(this)

        setContent {
            GeofenceRelayTheme {
                val context = this
                val prefs = remember { getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }
                val flavor = BuildConfig.FLAVOR

                var currentScreen by remember {
                    val isDeviceRemembered = prefs.getBoolean("is_device_remembered", false) || prefs.getBoolean("is_device_authenticated", false)
                    val savedEmail = prefs.getString("user_google_email", null)
                    val initialScreen: AppScreen = when (flavor) {
                        "tracker" -> {
                            if (isDeviceRemembered || !savedEmail.isNullOrBlank()) {
                                AppScreen.Tracker(savedEmail ?: "patient.device@smaran.local")
                            } else {
                                AppScreen.Auth(isTracker = true)
                            }
                        }
                        "guardian" -> {
                            if (isDeviceRemembered || !savedEmail.isNullOrBlank()) {
                                AppScreen.Guardian(savedEmail ?: "guardian.device@smaran.local")
                            } else {
                                AppScreen.Auth(isTracker = false)
                            }
                        }
                        else -> {
                            val savedRole = prefs.getString("user_role", null)
                            if ((isDeviceRemembered || !savedEmail.isNullOrBlank()) && savedRole == "guardian") {
                                AppScreen.Guardian(savedEmail ?: "guardian.device@smaran.local")
                            } else if (isDeviceRemembered || !savedEmail.isNullOrBlank()) {
                                AppScreen.Tracker(savedEmail ?: "patient.device@smaran.local")
                            } else {
                                AppScreen.RoleSelect
                            }
                        }
                    }
                    mutableStateOf(initialScreen)
                }

                when (val screen = currentScreen) {
                    is AppScreen.RoleSelect -> {
                        HomeScreen(
                            onSelectGuardian = {
                                prefs.edit().putString("user_role", "guardian").commit()
                                val isDeviceRemembered = prefs.getBoolean("is_device_remembered", false)
                                val savedEmail = prefs.getString("user_google_email", null)
                                currentScreen = if (isDeviceRemembered || !savedEmail.isNullOrBlank()) {
                                    AppScreen.Guardian(savedEmail ?: "guardian.device@smaran.local")
                                } else {
                                    AppScreen.Auth(isTracker = false)
                                }
                            },
                            onSelectTracker = {
                                prefs.edit().putString("user_role", "tracker").commit()
                                val isDeviceRemembered = prefs.getBoolean("is_device_remembered", false)
                                val savedEmail = prefs.getString("user_google_email", null)
                                currentScreen = if (isDeviceRemembered || !savedEmail.isNullOrBlank()) {
                                    AppScreen.Tracker(savedEmail ?: "patient.device@smaran.local")
                                } else {
                                    AppScreen.Auth(isTracker = true)
                                }
                            }
                        )
                    }
                    is AppScreen.Auth -> {
                        val isTracker = screen.isTracker
                        GoogleSignInScreen(
                            appTitle = "Smaran",
                            appSubtitle = if (isTracker)
                                "Dementia & Cognitive Care Companion\nAI Neuro-Telemetry, Reminiscence & GPS Sentinel"
                            else
                                "Caregiver Guardian Console\nLive Patient Radar & Cognitive Health Monitoring",
                            isTrackerMode = isTracker,
                            onSignInSuccess = { email ->
                                prefs.edit()
                                    .putBoolean("is_device_remembered", true)
                                    .putBoolean("is_device_authenticated", true)
                                    .putString("user_google_email", email)
                                    .putString("user_role", if (isTracker) "tracker" else "guardian")
                                    .commit()
                                currentScreen = if (isTracker) AppScreen.Tracker(email) else AppScreen.Guardian(email)
                            }
                        )
                    }
                    is AppScreen.Guardian -> {
                        BackHandler {
                            if (flavor.isEmpty()) {
                                currentScreen = AppScreen.RoleSelect
                            } else {
                                finish()
                            }
                        }
                        GuardianScreen(
                            googleAccountEmail = screen.email,
                            onBack = {
                                if (flavor.isEmpty()) {
                                    currentScreen = AppScreen.RoleSelect
                                } else {
                                    finish()
                                }
                            },
                            onSignOut = {
                                prefs.edit().clear().commit()
                                try {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                    ).requestEmail().build()
                                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut()
                                } catch (_: Exception) {}
                                currentScreen = if (flavor.isEmpty()) AppScreen.RoleSelect else AppScreen.Auth(isTracker = false)
                            }
                        )
                    }
                    is AppScreen.Tracker -> {
                        BackHandler {
                            if (flavor.isEmpty()) {
                                currentScreen = AppScreen.RoleSelect
                            } else {
                                finish()
                            }
                        }
                        val initialDest = if (intent?.getStringExtra("open_screen") == "games") {
                            intent?.removeExtra("open_screen")
                            net.kibotu.geofencerelay.features.ai.ui.SpringboardDestination.Exercises
                        } else {
                            net.kibotu.geofencerelay.features.ai.ui.SpringboardDestination.Home
                        }
                        SpringboardScreen(
                            userEmail = screen.email,
                            initialDestination = initialDest,
                            onSignOut = {
                                prefs.edit().clear().commit()
                                try {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                    ).requestEmail().build()
                                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut()
                                } catch (_: Exception) {}
                                currentScreen = if (flavor.isEmpty()) AppScreen.RoleSelect else AppScreen.Auth(isTracker = true)
                            }
                        )
                    }
                }
            }
        }
    }
}
