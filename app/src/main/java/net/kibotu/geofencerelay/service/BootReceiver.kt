package net.kibotu.geofencerelay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("BootReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            Log.d("BootReceiver", "Re-arming services and alarms for action: $action")

            val prefs = context.getSharedPreferences(
                TrackerForegroundService.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val isRunning = prefs.getBoolean(TrackerForegroundService.KEY_IS_RUNNING, false)

            if (isRunning) {
                Log.d("BootReceiver", "Resuming TrackerForegroundService...")
                try {
                    TrackerForegroundService.start(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to resume TrackerForegroundService: ${e.message}", e)
                }
            }

            // Reschedule Smaran Game Reminder Alarm so it persists across device reboots and app updates
            try {
                net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager.ensureAlarmScheduled(context)
                Log.d("BootReceiver", "Game reminder alarm verified and scheduled successfully.")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule game reminder: ${e.message}", e)
            }
        }
    }
}
