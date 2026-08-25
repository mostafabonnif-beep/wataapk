package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class AdminSecurityConfig(
    val masterLockEnabled: Boolean = false,
    val logs: List<String> = listOf(
        "تم تسجيل الدخول للوحة التحكم 🔐",
        "تأكيد حالة البث المباشر وقنوات يوتيوب 🔴"
    )
)
