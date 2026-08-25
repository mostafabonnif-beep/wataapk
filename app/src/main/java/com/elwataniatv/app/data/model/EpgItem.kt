package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class EpgItem(
    val id: String,
    val startTime: String, // "HH:MM"
    val title: String,
    val category: String = "",
    val duration: String = "",
    val description: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
)
