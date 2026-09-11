package net.kibotu.geofencerelay.features.ai.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import net.kibotu.geofencerelay.util.NotificationHelper
import net.kibotu.geofencerelay.util.SoundPlayer

object GameReminderManager {

    private const val PREFS_NAME = "game_reminder_prefs"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_LAST_PLAYED_TIME = "last_played_time"
    private const val KEY_IS_ALARM_FIRING = "is_alarm_firing"
    private const val KEY_NEXT_ALARM_TIME = "next_alarm_time"
    private const val REQUEST_CODE = 8801

    fun getReminderInterval(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_INTERVAL_MINUTES, 120L) // Default: 2 hours
    }

    fun setReminderInterval(context: Context, intervalMinutes: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_INTERVAL_MINUTES, intervalMinutes).apply()
        scheduleNextAlarm(context, intervalMinutes)
    }

    fun ensureAlarmScheduled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextAlarmTime = prefs.getLong(KEY_NEXT_ALARM_TIME, 0L)
        val now = System.currentTimeMillis()

        if (nextAlarmTime <= now) {
            Log.d("GameReminderManager", "No future alarm scheduled (time=$nextAlarmTime, now=$now). Scheduling now.")
            scheduleNextAlarm(context)
        } else {
            val intent = Intent(context, GameReminderReceiver::class.java).apply {
                action = "net.kibotu.geofencerelay.ACTION_GAME_REMINDER"
            }
            val flags = PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val existing = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
            if (existing == null) {
                Log.d("GameReminderManager", "PendingIntent was null, rescheduling.")
                scheduleNextAlarm(context)
            } else {
                Log.d("GameReminderManager", "Alarm is already actively scheduled for $nextAlarmTime")
            }
        }
    }

    fun scheduleNextAlarm(context: Context, intervalMinutes: Long = getReminderInterval(context)) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GameReminderReceiver::class.java).apply {
            action = "net.kibotu.geofencerelay.ACTION_GAME_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val triggerAtMillis = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)

        // Persist expected alarm trigger time so we can check it across reboots and app launches
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_NEXT_ALARM_TIME, triggerAtMillis)
            .apply()

        val showIntent = Intent(context, GameAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("GameReminderManager", "Scheduled next game reminder in $intervalMinutes minutes via GameReminderReceiver at $triggerAtMillis")
        } catch (e: Exception) {
            Log.w("GameReminderManager", "setAlarmClock failed (${e.message}), trying fallback...")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (ex: Exception) {
                Log.e("GameReminderManager", "Fallback alarm failed: ${ex.message}")
            }
        }
    }

    fun triggerTestAlarmInSeconds(context: Context, seconds: Int = 3) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (seconds * 1000L)

        val intent = Intent(context, GameReminderReceiver::class.java).apply {
            action = "net.kibotu.geofencerelay.ACTION_GAME_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val showIntent = Intent(context, GameAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            8803,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("GameReminderManager", "Scheduled test alarm for $seconds seconds via GameReminderReceiver")
        } catch (e: Exception) {
            Log.e("GameReminderManager", "Error scheduling test alarm: ${e.message}", e)
        }
    }

    fun recordGamePlayed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_PLAYED_TIME, System.currentTimeMillis())
            .putBoolean(KEY_IS_ALARM_FIRING, false)
            .apply()
        SoundPlayer.stopSound()
        scheduleNextAlarm(context)
    }

    fun isAlarmFiring(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ALARM_FIRING, false)
    }

    fun dismissAlarm(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_ALARM_FIRING, false).apply()
        SoundPlayer.stopSound()
        // Crucial: Automatically re-arm next alarm cycle so timer keeps running
        scheduleNextAlarm(context)
    }

    fun triggerReminderAlarm(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_ALARM_FIRING, true).apply()

        // 1. Play loud alarm sound (continuous looping)
        SoundPlayer.playFindMySound(context, loop = true)

        // 2. Fire high-priority reminder notification with lockscreen heads-up
        NotificationHelper.showGameReminderNotification(
            context,
            "🎮 Time for your Daily Brain Exercise! Tap to play and keep your mind active."
        )

        // 3. Crucial: Automatically schedule next alarm cycle so recurring timer never halts
        scheduleNextAlarm(context)
    }
}
