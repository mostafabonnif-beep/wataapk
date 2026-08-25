package com.elwataniatv.app.ui.screens.archive

/**
 * Normalizes Arabic text for forgiving local search without changing the text
 * displayed to the viewer.
 */
internal fun normalizeArabicSearchText(value: String): String = value
    .lowercase()
    .replace("ـ", "")
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace("أ", "ا")
    .replace("إ", "ا")
    .replace("آ", "ا")
    .replace("ى", "ي")
    .replace("ة", "ه")
    .replace(Regex("\\s+"), " ")
    .trim()
