package com.elwataniatv.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elwataniatv.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * notifications/FcmMessageService.kt
 * ─────────────────────────────────────────────────────────────────
 * يستقبل إشعارات FCM القادمة من دالة sendPushNotification
 * (functions/index.js) ويعرضها كإشعار نظام Android.
 * ─────────────────────────────────────────────────────────────────
 */
class FcmMessageService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmService"
        private const val CHANNEL_BREAKING = "breaking-news"
        private const val CHANNEL_PROGRAM = "program-reminders"
        private const val CHANNEL_STREAM = "stream-updates"
        private const val CHANNEL_GENERAL = "general"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "إشعار وارد: ${message.notification?.title}")

        createChannels()

        val title = message.notification?.title ?: getString(R.string.notification_default_title)
        val body = message.notification?.body ?: ""
        val category = message.data["category"] ?: NotificationPreferencesStore.CATEGORY_GENERAL
        if (!NotificationPreferencesStore.isCategoryEnabled(this, category)) {
            Log.i(TAG, "تم تجاهل إشعار الفئة المعطلة: $category")
            return
        }
        val channelId = when (category) {
            NotificationPreferencesStore.CATEGORY_BREAKING -> CHANNEL_BREAKING
            NotificationPreferencesStore.CATEGORY_PROGRAM -> CHANNEL_PROGRAM
            NotificationPreferencesStore.CATEGORY_STREAM -> CHANNEL_STREAM
            else -> CHANNEL_GENERAL
        }

        val intent = Intent(this, com.elwataniatv.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_screen", message.data["target_screen"] ?: category)
            message.data["stream_id"]?.let { putExtra("stream_id", it) }
            message.data["url"]?.let { putExtra("url", it) }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a monotonic, always-positive id: raw millis overflow Int.
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(notifId, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "توكن FCM جديد — إعادة التسجيل فوراً")
        // Re-register immediately so push keeps working after token rotation
        // (the app might be in background; this runs in the service process).
        com.elwataniatv.app.data.remote.FirebaseSync.registerFcmTokenNow()
    }

    private fun createChannels() {
        // NotificationChannel is API 26+ while minSdk is 24 — guard old devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val breaking = NotificationChannel(
            CHANNEL_BREAKING,
            getString(R.string.notification_channel_breaking),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_breaking_description)
            enableVibration(true)
        }

        val program = NotificationChannel(
            CHANNEL_PROGRAM,
            getString(R.string.notification_channel_program),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_program_description)
        }

        val stream = NotificationChannel(
            CHANNEL_STREAM,
            getString(R.string.notification_channel_stream),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_stream_description)
        }

        val general = NotificationChannel(
            CHANNEL_GENERAL,
            getString(R.string.notification_channel_general),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_general_description)
        }

        manager.createNotificationChannel(breaking)
        manager.createNotificationChannel(program)
        manager.createNotificationChannel(stream)
        manager.createNotificationChannel(general)
    }
}
