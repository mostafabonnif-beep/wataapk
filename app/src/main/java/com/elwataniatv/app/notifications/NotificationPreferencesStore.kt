package com.elwataniatv.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Local notification preferences used by FCM before a message is shown.
 * The global switch remains controlled by the Firebase-backed app preference;
 * category switches are intentionally local so they take effect immediately.
 */
object NotificationPreferencesStore {
    const val CATEGORY_BREAKING = "breaking"
    const val CATEGORY_PROGRAM = "program"
    const val CATEGORY_STREAM = "stream"
    const val CATEGORY_GENERAL = "general"

    private const val PREFS = "notification_preferences"
    private const val KEY_GLOBAL = "global_enabled"
    private const val KEY_REMOTE_GLOBAL = "remote_global_enabled"
    private const val KEY_BREAKING = "breaking_enabled"
    private const val KEY_PROGRAM = "program_enabled"
    private const val KEY_STREAM = "stream_enabled"

    fun isGlobalEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GLOBAL, true) &&
            prefs.getBoolean(KEY_REMOTE_GLOBAL, true)
    }

    fun setGlobalEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GLOBAL, enabled)
            .apply()
    }

    fun setRemoteGlobalEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMOTE_GLOBAL, enabled)
            .apply()
    }

    fun hasSystemPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun isCategoryEnabled(context: Context, category: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!isGlobalEnabled(context) || !hasSystemPermission(context)) return false
        return when (category.lowercase()) {
            CATEGORY_BREAKING -> prefs.getBoolean(KEY_BREAKING, true)
            CATEGORY_PROGRAM -> prefs.getBoolean(KEY_PROGRAM, true)
            CATEGORY_STREAM -> prefs.getBoolean(KEY_STREAM, true)
            else -> true
        }
    }

    fun setCategoryEnabled(context: Context, category: String, enabled: Boolean) {
        val key = when (category.lowercase()) {
            CATEGORY_BREAKING -> KEY_BREAKING
            CATEGORY_PROGRAM -> KEY_PROGRAM
            CATEGORY_STREAM -> KEY_STREAM
            else -> return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, enabled)
            .apply()
    }
}
