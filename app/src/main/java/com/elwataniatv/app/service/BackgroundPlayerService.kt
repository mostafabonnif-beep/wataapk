package com.elwataniatv.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.elwataniatv.app.MainActivity
import com.elwataniatv.app.R

class BackgroundPlayerService : Service() {

    private val CHANNEL_ID = "elwatania_media_channel"
    private val NOTIFICATION_ID = 1001

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var focusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    companion object {
        const val ACTION_PLAY = "com.elwataniatv.app.action.PLAY"
        const val ACTION_PAUSE = "com.elwataniatv.app.action.PAUSE"
        const val ACTION_TOGGLE = "com.elwataniatv.app.action.TOGGLE"
        const val ACTION_STOP = "com.elwataniatv.app.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, BackgroundPlayerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.startService(intent) }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundPlayerService::class.java)
            runCatching { context.stopService(intent) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        setupAudio()
        createNotificationChannel()
    }

    private fun setupAudio() {
        focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            val player = BackgroundPlayerManager.activePlayer
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    player?.let {
                        it.volume = 1.0f
                        if (!it.isPlaying) {
                            it.play()
                            BackgroundPlayerManager.updatePlayingState(true)
                            updateNotification(true)
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    player?.let {
                        it.pause()
                        BackgroundPlayerManager.updatePlayingState(false)
                        updateNotification(false)
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    player?.let {
                        it.pause()
                        BackgroundPlayerManager.updatePlayingState(false)
                        updateNotification(false)
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    player?.volume = 0.3f
                }
            }
        }

        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusChangeListener!!)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                BackgroundPlayerManager.play()
                updateNotification(true)
            }
            ACTION_PAUSE -> {
                BackgroundPlayerManager.pause()
                updateNotification(false)
            }
            ACTION_TOGGLE -> {
                val isPlaying = BackgroundPlayerManager.activePlayer?.isPlaying == true
                if (isPlaying) {
                    BackgroundPlayerManager.pause()
                    updateNotification(false)
                } else {
                    BackgroundPlayerManager.play()
                    updateNotification(true)
                }
            }
            ACTION_STOP -> {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
        }

        val isPlaying = BackgroundPlayerManager.activePlayer?.isPlaying == true
        startForeground(NOTIFICATION_ID, buildNotification(isPlaying))
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "البث المباشر (Background Playback)"
            val descriptionText = "التحكم في تشغيل بث قناة الوطنية TV في الخلفية"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "إيقاف مؤقت",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "تشغيل",
                createPendingIntent(ACTION_PLAY)
            )
        }

        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "إيقاف",
            createPendingIntent(ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("قناة الوطنية TV")
            .setContentText(if (isPlaying) "البث المباشر يعمل..." else "متوقف مؤقتاً")
            .setSmallIcon(R.mipmap.ic_launcher_watania)
            .setContentIntent(pendingIntent)
            .addAction(toggleAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
            )
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, BackgroundPlayerService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopPlaybackAndService() {
        BackgroundPlayerService.stop(this)
        BackgroundPlayerManager.stopAndRelease()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
        }
    }
}
