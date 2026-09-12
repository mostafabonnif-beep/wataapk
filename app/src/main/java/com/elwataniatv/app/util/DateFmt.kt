package com.elwataniatv.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Arabic date formatting for the Algerian audience: Algerian month names
 * (French-derived) plus relative day rules (اليوم / أمس / منذ X أيام...).
 */
object DateFmt {

    private val ALGERIAN_MONTHS = arrayOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val ISO_DATETIME_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    /** Parses an ISO date, ISO datetime, or epoch-millis string; null when unparseable. */
    fun parse(raw: String): LocalDate? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { epoch ->
            return try {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(epoch),
                    ZoneId.of("Africa/Algiers")
                ).toLocalDate()
            } catch (_: Exception) {
                null
            }
        }
        return try {
            LocalDate.parse(trimmed, ISO_DATE)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(trimmed, ISO_DATETIME_MILLIS).toLocalDate()
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(trimmed, ISO_DATETIME).toLocalDate()
                } catch (_: Exception) {
                    try {
                        LocalDateTime.parse(trimmed).toLocalDate()
                    } catch (_: Exception) {
                        try {
                            java.time.OffsetDateTime.parse(trimmed).toLocalDate()
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
    }

    /** "اليوم", "أمس", "منذ 3 أيام", "منذ 2 أسبوع", "25 جويلية", "25 جويلية 2025". */
    fun smartDate(raw: String, today: LocalDate = LocalDate.now(ZoneId.of("Africa/Algiers"))): String {
        val date = parse(raw) ?: return raw
        val days = java.time.temporal.ChronoUnit.DAYS.between(date, today)
        return when {
            days <= -1L -> formatDayMonthYear(date) // future-dated content
            days == 0L -> "اليوم"
            days == 1L -> "أمس"
            days < 7L -> if (days == 2L) "منذ يومين" else "منذ $days أيام"
            days <= 30L -> {
                val weeks = days / 7
                if (weeks == 1L) "منذ أسبوع" else if (weeks == 2L) "منذ أسبوعين" else "منذ $weeks أسابيع"
            }
            date.year == today.year -> formatDayMonth(date)
            else -> formatDayMonthYear(date)
        }
    }

    /** Always "25 جويلية 2026" — used where relative labels would confuse. */
    fun fullDate(raw: String): String {
        val date = parse(raw) ?: return raw
        return formatDayMonthYear(date)
    }

    private fun formatDayMonth(date: LocalDate): String =
        "${date.dayOfMonth} ${ALGERIAN_MONTHS[date.monthValue - 1]}"

    private fun formatDayMonthYear(date: LocalDate): String =
        "${date.dayOfMonth} ${ALGERIAN_MONTHS[date.monthValue - 1]} ${date.year}"
}
