package com.elwataniatv.app.util

/**
 * Centralized content hygiene rules shared by remote sync and Compose screens.
 * This is intentionally conservative: it rejects only known placeholder records
 * and empty values, while leaving editorial content under Firebase control.
 */
object ContentSanitizer {
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun normalized(value: String): String = value.trim().lowercase().replace(separators, "")

    fun isKnownTestValue(value: String): Boolean {
        return normalized(value) in setOf("oussama", "oussamab", "200")
    }

    fun isUsable(value: String): Boolean {
        return value.isNotBlank() && !isKnownTestValue(value)
    }
}
