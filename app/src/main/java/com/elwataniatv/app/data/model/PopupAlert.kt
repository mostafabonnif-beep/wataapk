package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class PopupAlert(
    val active: Boolean = false,
    val title: String = "",
    val message: String = "",
    val buttonText: String = "",
    val alertType: String = "info" // "info", "warning", "breaking", "event"
)
