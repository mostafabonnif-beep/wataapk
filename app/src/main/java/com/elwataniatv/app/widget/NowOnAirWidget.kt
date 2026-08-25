package com.elwataniatv.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.elwataniatv.app.MainActivity
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.EpgItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Home-screen widget showing what is currently on air.
 *
 * The app refreshes the widget whenever the EPG syncs ([refresh]); the widget
 * itself also refreshes on its updatePeriodMillis (Android: 30 min minimum)
 * by reading the latest EPG from the shared prefs cache.
 */
class NowOnAirWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget instance is added.
    }

    companion object {

        private const val PREFS = "widget_prefs"
        private const val KEY_EPG_JSON = "epg_json"
        private const val KEY_CHANNEL = "channel_name"

        /** Widget update entry point called by the app after each EPG sync. */
        fun refresh(context: Context, epgList: List<EpgItem>, channelName: String) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_EPG_JSON, kotlinx.serialization.json.Json.encodeToString(epgList))
                .putString(KEY_CHANNEL, channelName)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NowOnAirWidget::class.java))
            if (ids.isNotEmpty()) {
                ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val channel = prefs.getString(KEY_CHANNEL, "").orEmpty()
                .ifBlank { context.getString(R.string.app_name) }
            val epgJson = prefs.getString(KEY_EPG_JSON, null)
            val epg: List<EpgItem> = if (epgJson.isNullOrBlank()) {
                emptyList()
            } else {
                runCatching {
                    kotlinx.serialization.json.Json.decodeFromString<List<EpgItem>>(epgJson)
                }.getOrDefault(emptyList())
            }

            val nowMinutes = minutesOfDay()
            val current = epg
                .filter { it.isActive }
                .sortedBy { it.startTime }
                .lastOrNull { minutesOfDay(it.startTime) <= nowMinutes }
            val next = epg
                .filter { it.isActive }
                .sortedBy { it.startTime }
                .firstOrNull { minutesOfDay(it.startTime) > nowMinutes }

            val views = RemoteViews(context.packageName, R.layout.widget_now_on_air)
            views.setTextViewText(R.id.widget_channel_name, channel)
            views.setTextViewText(
                R.id.widget_program_title,
                current?.title ?: context.getString(R.string.widget_no_program)
            )
            views.setTextViewText(
                R.id.widget_program_time,
                if (current != null && next != null) {
                    context.getString(
                        R.string.widget_program_time_range,
                        current.startTime,
                        next.startTime
                    )
                } else {
                    context.getString(R.string.widget_program_live)
                }
            )

            // Tap → open the app on the live screen.
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_channel_name, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_program_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_program_time, pendingIntent)
            return views
        }

        private fun minutesOfDay(time: String = ""): Int {
            if (time.isBlank()) return -1
            val parts = time.trim().split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
        }
    }
}
