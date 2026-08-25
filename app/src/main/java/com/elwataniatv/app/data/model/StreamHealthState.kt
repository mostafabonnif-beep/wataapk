package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class StreamHealthState(
    val isLiveActive: Boolean = false,
    val isYoutubeLiveActive: Boolean = false,
    val hlsPingLatencyMs: Long = -1,
    val lastCheckTimestamp: String = "لم يتم الفحص بعد",
    val statusMessage: String = "لم يتم فحص البث بعد",
    val autoCheckEnabled: Boolean = true
)
