package net.kibotu.geofencerelay.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

object SoundPlayer {

    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: MediaPlayer? = null

    fun playFindMySound(context: Context, loop: Boolean = true) {
        stopSound()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context.applicationContext, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = loop
                    prepare()
                    start()
                }
                activeMediaPlayer = mp
                Log.d("SoundPlayer", "Playing alarm sound via MediaPlayer (looping=$loop)")
            } catch (e: Exception) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.audioAttributes = audioAttributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = loop
                }
                ringtone.play()
                activeRingtone = ringtone
                Log.d("SoundPlayer", "Playing alarm sound via Ringtone fallback")
            }
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Failed to play sound: ${e.message}", e)
        }
    }

    fun stopSound() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        activeMediaPlayer = null

        try {
            activeRingtone?.stop()
        } catch (_: Exception) {}
        activeRingtone = null
    }
}

