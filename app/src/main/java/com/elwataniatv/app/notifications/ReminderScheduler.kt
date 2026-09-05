package com.elwataniatv.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.elwataniatv.app.R
import com.elwataniatv.app.data.local.ProgramReminder
import java.util.Calendar

/**
 * Real EPG program reminders.
 *
 * Stores nothing itself — it schedules an intentionally inexact daily alarm
 * per ProgramReminder stored in Room. When the alarm fires, [ReminderReceiver]
 * posts a system notification and schedules the next occurrence. On device
 * reboot [BootReceiver] restores every enabled reminder without requiring the
 * special exact-alarm permission.
 */
object ReminderScheduler {

    const val CHANNEL_REMINDERS = "program-reminders"
    internal const val EXTRA_ID = "program_id"
    internal const val EXTRA_TITLE = "program_title"
    internal const val EXTRA_TIME = "program_time"
    private const val REQUEST_CODE_BASE = 5000

    fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** Schedules (or reschedules) the inexact alarm for a single reminder. */
    fun schedule(context: Context, reminder: ProgramReminder) {
        if (!reminder.enabled) {
            cancel(context, reminder)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(reminder.startTime),
            pendingIntent(context, reminder)
        )
    }

    /** Cancels the alarm for a reminder. */
    fun cancel(context: Context, reminder: ProgramReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, reminder))
    }

    /** Reschedules every stored reminder (app start / boot). Idempotent. */
    fun scheduleAll(context: Context, reminders: List<ProgramReminder>) {
        reminders.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, reminder: ProgramReminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.programTitle)
            putExtra(EXTRA_TIME, reminder.startTime)
        }
        val requestCode = REQUEST_CODE_BASE + Math.floorMod(reminder.id.hashCode(), 100_000)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Next occurrence of "HH:MM": today if it is still in the future,
     * otherwise tomorrow (daily schedule).
     */
    fun nextTriggerMillis(startTime: String, nowMillis: Long = System.currentTimeMillis()): Long {
        val parts = startTime.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            .apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

/** Fires the actual notification when a reminder alarm goes off. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_ID)
                if (!reminderId.isNullOrBlank()) {
                    try {
                        val db = com.elwataniatv.app.data.local.AppDatabase.getDatabase(context)
                        val reminder = db.remindersDao().getReminderByIdSync(reminderId)
                        if (reminder?.enabled != true) return@execute
                        ReminderScheduler.schedule(context, reminder)
                    } catch (error: Exception) {
                        android.util.Log.w(
                            "ReminderReceiver",
                            "تعذر إعادة جدولة التذكير: ${error.message}"
                        )
                        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
                        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME)
                        if (!title.isNullOrBlank() && !time.isNullOrBlank()) {
                            runCatching {
                                ReminderScheduler.schedule(
                                    context,
                                    ProgramReminder(
                                        id = reminderId,
                                        programTitle = title,
                                        startTime = time,
                                    ),
                                )
                            }.onFailure { scheduleError ->
                                android.util.Log.w(
                                    "ReminderReceiver",
                                    "تعذر حفظ موعد التذكير التالي: ${scheduleError.message}",
                                )
                            }
                        }
                    }
                }
                postNotification(context, intent)
            } finally {
                executor.shutdown()
                pending.finish()
            }
        }
    }

    private fun postNotification(context: Context, intent: Intent) {
        ReminderScheduler.ensureChannel(context)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
            ?: context.getString(R.string.reminder_default_title)
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME).orEmpty()

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, com.elwataniatv.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_screen", "live")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                if (time.isNotBlank()) {
                    context.getString(R.string.reminder_starts_now_at, time)
                } else {
                    context.getString(R.string.reminder_starts_now)
                }
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.reminder_body, title)
                )
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

/** Reschedules all stored reminders after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val db = com.elwataniatv.app.data.local.AppDatabase.getDatabase(context)
                val reminders = db.remindersDao().getAllRemindersSync()
                ReminderScheduler.scheduleAll(context, reminders)
            } catch (e: Exception) {
                android.util.Log.w("BootReceiver", "فشل إعادة جدولة التذكيرات: ${e.message}")
            } finally {
                executor.shutdown()
                pending.finish()
            }
        }
    }
}
