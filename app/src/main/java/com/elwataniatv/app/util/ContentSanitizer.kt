package com.elwataniatv.app.util

/**
 * Centralized content hygiene rules shared by remote sync and Compose screens.
 * This is intentionally conservative: it rejects only known placeholder records
 * and empty values, while leaving editorial content under Firebase control.
 */
object ContentSanitizer {
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val knownTestValues = setOf(
        "oussama",
        "oussamab",
        "200",
        "demo",
        "dummy",
        "placeholder",
        "sample",
        "test",
        "testing",
        "تجريبي",
        "تجربة"
    )

    fun normalized(value: String): String = value.trim().lowercase().replace(separators, "")

    fun isKnownTestValue(value: String): Boolean {
        val normalizedValue = normalized(value)
        return normalizedValue in knownTestValues ||
            knownTestValues.any { marker -> marker.length >= 4 && normalizedValue.contains(marker) }
    }

    fun isUsable(value: String): Boolean {
        return value.isNotBlank() && !isKnownTestValue(value)
    }
}
