package net.kibotu.geofencerelay.features.ai.reminder

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.features.ai.ui.theme.GoogleColors
import net.kibotu.geofencerelay.ui.MainActivity
import net.kibotu.geofencerelay.ui.theme.GeofenceRelayTheme
import net.kibotu.geofencerelay.util.SoundPlayer

import android.os.PowerManager
import android.util.Log
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.util.NotificationHelper

/**
 * Dedicated Full-Screen Activity that wakes the device and turns the screen on
 * over the lock screen even when the app is closed, killed, or phone screen is off.
 * Plays continuous loud alarm sound and renders popping Google colors.
 */
class GameAlarmActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set lockscreen display and turn screen on before AND after super.onCreate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        super.onCreate(savedInstanceState)

        // 1. Physically force display hardware ON from sleep via PowerManager
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Smaran:AlarmActivityWakeLock"
            )
            wakeLock?.acquire(30000L)
            Log.d("GameAlarmActivity", "Physical screen wake lock acquired successfully")
        } catch (e: Exception) {
            Log.e("GameAlarmActivity", "WakeLock error: ${e.message}")
        }

        // Re-confirm window flags after view creation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // 2. Start looping alarm audio immediately
        SoundPlayer.playFindMySound(this, loop = true)

        // 3. Fire high-priority reminder notification as well
        try {
            NotificationHelper.showGameReminderNotification(
                this,
                "🎮 Time for your Daily Brain Exercise! Tap to play and keep your mind active."
            )
        } catch (_: Exception) {}

        setContent {
            GeofenceRelayTheme {
                LockScreenAlarmView(
                    onPlayGame = {
                        SoundPlayer.stopSound()
                        GameReminderManager.dismissAlarm(this)
                        try {
                            NotificationHelper.cancelGameReminderNotification(this)
                        } catch (_: Exception) {}

                        val launchIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("open_screen", "games")
                        }

                        // Dismiss keyguard ONLY when user taps "START BRAIN GAME NOW"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                            km?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                                override fun onDismissSucceeded() {
                                    startActivity(launchIntent)
                                    finish()
                                }
                                override fun onDismissCancelled() {
                                    startActivity(launchIntent)
                                    finish()
                                }
                                override fun onDismissError() {
                                    startActivity(launchIntent)
                                    finish()
                                }
                            }) ?: run {
                                startActivity(launchIntent)
                                finish()
                            }
                        } else {
                            startActivity(launchIntent)
                            finish()
                        }
                    },
                    onSnooze = {
                        SoundPlayer.stopSound()
                        GameReminderManager.dismissAlarm(this)
                        try {
                            NotificationHelper.cancelGameReminderNotification(this)
                        } catch (_: Exception) {}
                        // Snooze for 10 minutes
                        GameReminderManager.scheduleNextAlarm(this, 10L)
                        finish()
                    }
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do NOT dismiss alarm on back press! Require explicit user button tap (Play or Snooze)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            SoundPlayer.stopSound()
        }
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}

@Composable
private fun LockScreenAlarmView(
    onPlayGame: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.activity.compose.BackHandler(enabled = true) {
        // Do not dismiss alarm on back press/gesture
    }
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val selectedLanguageCode = remember { prefs.getString("selected_language", "en") ?: "en" }

    // Continuous infinite pulsation for popping Google colors
    val infiniteTransition = rememberInfiniteTransition(label = "poppingColors")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    // Animated color cycle between Google Brand Colors
    val colorStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorStep"
    )

    val currentColor = when (colorStep.toInt() % 4) {
        0 -> GoogleColors.Blue
        1 -> GoogleColors.Red
        2 -> GoogleColors.Yellow
        else -> GoogleColors.Green
    }

    val animatedBgColor by animateColorAsState(
        targetValue = currentColor,
        animationSpec = tween(500),
        label = "bgAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        animatedBgColor.copy(alpha = 0.85f),
                        Color(0xFF1A1A24)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Smaran Branding Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.smaran_logo),
                    contentDescription = "Smaran Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Smaran",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Popping radiating rings behind alarm bell
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Expanding wave ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .border(4.dp, Color.White.copy(alpha = ringAlpha), CircleShape)
                )

                // Inner pulsing glowing circle
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alarm Active",
                        tint = animatedBgColor,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text Banner
            Text(
                text = MultilingualManager.tr("alarm_title", selectedLanguageCode),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = MultilingualManager.tr("alarm_desc", selectedLanguageCode),
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Giant "START BRAIN GAME NOW" Button (Solid bounds for 100% reliable finger taps)
            Button(
                onClick = onPlayGame,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleColors.Green),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(pulseScale)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = MultilingualManager.tr("alarm_btn_play", selectedLanguageCode),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Snooze 10 Minutes Button
            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Snooze,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = MultilingualManager.tr("alarm_btn_snooze", selectedLanguageCode),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}