package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class RemoteStream(
    val id: String,
    val title: String,
    val url: String,
    val type: String = "m3u8", // "m3u8", "youtube", "other"
    val logoUrl: String = "",
    val isActive: Boolean = true,
    val order: Int = 0
)
