package com.elwataniatv.app.util

import android.app.PictureInPictureParams
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State

/**
 * PictureInPicture support for the live stream.
 *
 * The player (deep inside the Compose tree) reports whether live playback is
 * active through [setLivePlaying]; MainActivity reads that flag in
 * onUserLeaveHint and enters PIP automatically. [isInPip] is a Compose state
 * so the player UI can hide its controls while the activity is in the small
 * floating window.
 */
object PipController {

    @Volatile
    var livePlaying: Boolean = false
        private set

    var isInPip: State<Boolean> = mutableStateOf(false)
        private set

    private val _inPip = mutableStateOf(false)

    init {
        isInPip = _inPip
    }

    /** Called by VideoPlayerView when live playback starts/stops. */
    fun setLivePlaying(playing: Boolean) {
        livePlaying = playing
    }

    /** Called by MainActivity.onPictureInPictureModeChanged. */
    fun onPipModeChanged(inPip: Boolean) {
        _inPip.value = inPip
    }

    /** Aspect ratio of the floating window: 16:9 like the stream. */
    fun pipParams(sourceRectHint: android.graphics.Rect? = null): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                    if (sourceRectHint != null) setSourceRectHint(sourceRectHint)
                }
            }
            .build()
    }

    /** True when PIP is a supported, available feature on this device. */
    fun isAvailable(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** Enters PIP when live playback is active and PIP is available. */
    @android.annotation.SuppressLint("NewApi")
    fun enterIfLivePlaying(activity: Activity) {
        if (!isAvailable(activity)) return
        if (!livePlaying) return
        if (activity.isInPictureInPictureMode) return
        pipParams()?.let { params ->
            activity.enterPictureInPictureMode(params)
        }
    }

    /** Sets the smooth auto-enter PIP params on Android 12+ from the player view. */
    @android.annotation.SuppressLint("NewApi")
    fun updateAutoEnter(activity: Activity, sourceRect: android.graphics.Rect?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        pipParams(sourceRect)?.let { params ->
            runCatching { activity.setPictureInPictureParams(params) }
        }
    }

    /** Convenience: whether the activity is in PIP mode. */
    fun isInPip(activity: Activity): Boolean =
        activity.isInPictureInPictureMode
}
