package net.kibotu.geofencerelay.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.ui.MainActivity

object NotificationHelper {

    const val SERVICE_CHANNEL_ID = "geofence_service_channel"
    const val BREACH_CHANNEL_ID = "geofence_breach_channel"
    const val GAME_ALARM_CHANNEL_ID = "smaran_game_alarm_channel_v4"
    const val SERVICE_NOTIFICATION_ID = 1001
    const val BREACH_NOTIFICATION_ID = 2001
    const val GAME_ALARM_NOTIFICATION_ID = 3001
    const val ACTION_SNOOZE = "net.kibotu.geofencerelay.ACTION_SNOOZE_ALARM"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Geofence Sentinel Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent background location monitoring status"
                setShowBadge(false)
            }

            val breachChannel = NotificationChannel(
                BREACH_CHANNEL_ID,
                "Geofence Breach Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when target device breaches safe geofence"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val alarmChannel = NotificationChannel(
                GAME_ALARM_CHANNEL_ID,
                "Smaran Brain Exercise Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm and lockscreen wake-up alerts for scheduled brain games"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM), audioAttributes)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(breachChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun buildServiceNotification(
        context: Context,
        isBreached: Boolean,
        statusText: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBreached) "🚨 SAFE ZONE BREACH DETECTED!" else "🛡️ Smaran Sentinel Active"

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (isBreached) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showBreachNotification(
        context: Context,
        geofenceName: String,
        distanceMeters: Double,
        deviceName: String = "Tracked Device"
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Wake screen up if phone is in off / sleep mode
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "GeofenceRelay:BreachWakeLock"
            )
            wakeLock?.acquire(3500L)
        } catch (_: Exception) {}

        val distText = LocationUtils.formatDistance(distanceMeters)
        val notification = NotificationCompat.Builder(context, BREACH_CHANNEL_ID)
            .setContentTitle("🚨 SAFE ZONE BREACH DETECTED!")
            .setContentText("$deviceName is outside '$geofenceName' ($distText away)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("⚠️ Alert: $deviceName has moved outside the designated safe zone '$geofenceName' by $distText.\nLive GPS tracking is active."))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Heads-up banner popup on lock screen & off mode
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(BREACH_NOTIFICATION_ID, notification)
    }

    fun cancelBreachNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(BREACH_NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun cancelGameReminderNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(GAME_ALARM_NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun showGameReminderNotification(context: Context, message: String) {
        createNotificationChannels(context)

        val alarmIntent = Intent(context, net.kibotu.geofencerelay.features.ai.reminder.GameAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            GAME_ALARM_NOTIFICATION_ID,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1-Tap Play Now Action
        val playIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "games")
        }
        val playPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1-Tap Snooze Action
        val snoozeIntent = Intent(context, net.kibotu.geofencerelay.features.ai.reminder.GameReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Wake screen up physically if phone is asleep
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "Smaran:GameReminderWakeLock"
            )
            wakeLock?.acquire(15000L)
        } catch (_: Exception) {}

        val alarmSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(context, GAME_ALARM_CHANNEL_ID)
            .setContentTitle("🎮 Smaran Brain Exercise Reminder")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSoundUri)
            .setVibrate(longArrayOf(0, 600, 250, 600))
            .addAction(android.R.drawable.ic_media_play, "▶️ Play Now", playPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "⏰ Snooze 10m", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GAME_ALARM_NOTIFICATION_ID, notification)
    }
}
