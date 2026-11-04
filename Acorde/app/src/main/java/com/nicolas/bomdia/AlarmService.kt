package com.nicolas.bomdia

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import kotlin.math.PI
import kotlin.math.sin

class AlarmService : Service() {
    private var track: AudioTrack? = null
    private var vib: Vibrator? = null
    private val h = Handler(Looper.getMainLooper())

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val label = i?.getStringExtra("l") ?: "Acordar!"
        val time = i?.getStringExtra("t") ?: ""
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel("alarm", "Alarme", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
        val ai = Intent(this, AlarmActivity::class.java)
            .putExtra("l", label).putExtra("t", time)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(this, 2, ai, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("$time — toque para desligar")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(1, n)
        play()
        h.removeCallbacksAndMessages(null)
        h.postDelayed({ stopSelf() }, 10 * 60 * 1000L) // desliga sozinho em 10 min
        return START_NOT_STICKY
    }

    private fun play() {
        stopSound()
        // volume do ALARME no máximo, ignora o volume de mídia
        val am = getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)

        // sirene gerada no código: 900 Hz / 1500 Hz alternando, onda quadrada
        val sr = 22050
        val buf = ShortArray(sr)
        for (k in 0 until sr) {
            val f = if ((k * 4 / sr) % 2 == 0) 900.0 else 1500.0
            buf[k] = (if (sin(2 * PI * f * k / sr) >= 0) 30000 else -30000).toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(buf, 0, buf.size)
        t.setLoopPoints(0, buf.size, -1)
        t.play()
        track = t

        @Suppress("DEPRECATION")
        vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 300), 0))
    }

    private fun stopSound() {
        try { track?.stop(); track?.release() } catch (e: Exception) {}
        track = null
        vib?.cancel()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        stopSound()
        super.onDestroy()
    }
}
