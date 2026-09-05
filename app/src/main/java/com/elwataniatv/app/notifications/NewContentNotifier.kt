package com.elwataniatv.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.ArchiveProgram

/**
 * Posts a single system notification when the editorial team publishes new
 * archive content while the app is running. Respects the user's notification
 * permission state; never fabricates content when the list is empty.
 */
object NewContentNotifier {

    const val CHANNEL_NEW_CONTENT = "new-content"
    private const val REQUEST_CODE = 7001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_NEW_CONTENT,
            context.getString(R.string.new_content_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.new_content_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Returns true when a notification was posted for the newly added
     * programs. [knownIds] is the set of program ids already seen; pass null
     * on the first snapshot so the initial load never notifies.
     */
    fun notifyNewArchivePrograms(
        context: Context,
        programs: List<ArchiveProgram>,
        knownIds: Set<String>?,
    ): Set<String> {
        val currentIds = programs.map { it.id }.toSet()
        if (knownIds == null) return currentIds
        val added = programs.filter { it.id !in knownIds }
        if (added.isEmpty()) return currentIds

        val manager = NotificationManagerCompat.from(context)
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || !manager.areNotificationsEnabled()) return currentIds

        ensureChannel(context)
        val first = added.first()
        val title = context.getString(R.string.new_content_notification_title)
        val text = if (added.size == 1) {
            first.title
        } else {
            context.getString(R.string.new_content_notification_summary, first.title, added.size - 1)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, com.elwataniatv.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_screen", "archive")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_CONTENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(REQUEST_CODE, notification) }
        return currentIds
    }
}
