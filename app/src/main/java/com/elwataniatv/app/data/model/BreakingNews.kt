package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class BreakingNews(
    val enabled: Boolean = false,
    val text: String = "",
    val youtubeUrl: String = ""
)
