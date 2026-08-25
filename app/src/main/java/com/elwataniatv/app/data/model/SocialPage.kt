package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class SocialPage(
    val id: String,
    val platform: String, // "Facebook", "Instagram", "X", "YouTube", "TikTok", "Telegram"
    val name: String,
    val url: String,
    val description: String = "",
    val logoUrl: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    val emoji: String = "",
    val color: String = "#1877F2"
)
