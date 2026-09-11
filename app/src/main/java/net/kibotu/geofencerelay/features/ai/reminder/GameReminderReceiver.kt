package net.kibotu.geofencerelay.features.ai.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import net.kibotu.geofencerelay.util.NotificationHelper
import net.kibotu.geofencerelay.util.SoundPlayer

class GameReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("GameReminderReceiver", "Alarm trigger received! Action: ${intent?.action}")

        // Handle 1-Tap Snooze action from notification button
        if (intent?.action == NotificationHelper.ACTION_SNOOZE) {
            Log.d("GameReminderReceiver", "Snooze action triggered from notification")
            SoundPlayer.stopSound()
            NotificationHelper.cancelGameReminderNotification(context)
            GameReminderManager.dismissAlarm(context)
            GameReminderManager.scheduleNextAlarm(context, 10L)
            return
        }

        // 1. Wake screen up immediately using PowerManager WakeLock
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "Smaran:AlarmReceiverWakeLock"
            )
            wakeLock?.acquire(15000L)
            Log.d("GameReminderReceiver", "Acquired screen wake lock from receiver")
        } catch (e: Exception) {
            Log.e("GameReminderReceiver", "WakeLock error: ${e.message}")
        }

        // 2. Play sound and fire heads-up notification with Full-Screen Intent
        // This also automatically schedules the next recurring alarm cycle
        GameReminderManager.triggerReminderAlarm(context)

        // 3. Attempt direct activity launch to show full-screen popping colors
        try {
            val alarmIntent = Intent(context, GameAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(alarmIntent)
            Log.d("GameReminderReceiver", "Started GameAlarmActivity from receiver")
        } catch (e: Exception) {
            Log.e("GameReminderReceiver", "Failed to start GameAlarmActivity directly: ${e.message}", e)
        }
    }
}
