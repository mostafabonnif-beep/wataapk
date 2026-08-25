package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class AdBanner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val targetUrl: String,
    val isEnabled: Boolean = true,
    val order: Int = 0
)
