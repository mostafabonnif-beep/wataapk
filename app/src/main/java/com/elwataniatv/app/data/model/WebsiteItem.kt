package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class WebsiteItem(
    val id: String,
    val name: String,
    val url: String,
    val description: String = "",
    val logoUrl: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    val emoji: String = "",
    val color: String = "#0a7ea4"
)
