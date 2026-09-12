package com.elwataniatv.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Arabic date formatting for the Algerian audience: Algerian month names
 * (French-derived) plus relative day rules (اليوم / أمس / منذ X أيام...).
 *
 * Implemented with Calendar/SimpleDateFormat (API 24-safe, no java.time).
 */
object DateFmt {

    private val ALGERIAN_MONTHS = arrayOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val ALGIERS = TimeZone.getTimeZone("Africa/Algiers")

    private val ISO_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = ALGIERS
        isLenient = false
    }
    private val ISO_DATETIME = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = ALGIERS
        isLenient = false
    }
    private val ISO_DATETIME_MILLIS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
        timeZone = ALGIERS
        isLenient = false
    }

    /** Parses an ISO date, ISO datetime, or epoch-millis string; null when unparseable. */
    fun parse(raw: String): Calendar? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { epoch ->
            return Calendar.getInstance(ALGIERS).apply { timeInMillis = epoch }
        }
        for (format in listOf(ISO_DATE, ISO_DATETIME_MILLIS, ISO_DATETIME)) {
            try {
                val parsed: Date = format.parse(trimmed) ?: continue
                return Calendar.getInstance(ALGIERS).apply { time = parsed }
            } catch (_: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    /** "اليوم", "أمس", "منذ 3 أيام", "منذ 2 أسبوع", "25 جويلية", "25 جويلية 2025". */
    fun smartDate(raw: String, today: Calendar = Calendar.getInstance(ALGIERS)): String {
        val date = parse(raw) ?: return raw
        val dayOfDate = floorDivDay(date)
        val dayOfToday = floorDivDay(today)
        val days = ((dayOfToday - dayOfDate) / DAY_MS)
        return when {
            days <= -1L -> formatDayMonthYear(date) // future-dated content
            days == 0L -> "اليوم"
            days == 1L -> "أمس"
            days == 2L -> "منذ يومين"
            days < 7L -> "منذ $days أيام"
            days <= 30L -> {
                val weeks = days / 7
                if (weeks == 1L) "منذ أسبوع" else if (weeks == 2L) "منذ أسبوعين" else "منذ $weeks أسابيع"
            }
            date.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> formatDayMonth(date)
            else -> formatDayMonthYear(date)
        }
    }

    /** Always "25 جويلية 2026" — used where relative labels would confuse. */
    fun fullDate(raw: String): String {
        val date = parse(raw) ?: return raw
        return formatDayMonthYear(date)
    }

    // Calendar day floored to UTC midnight, in millis — stable day arithmetic
    // across DST boundary crossings (Algiers has no DST, but stay defensive).
    private fun floorDivDay(cal: Calendar): Long {
        val copy = cal.clone() as Calendar
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return copy.timeInMillis
    }

    private const val DAY_MS = 86_400_000L

    private fun formatDayMonth(date: Calendar): String =
        "${date.get(Calendar.DAY_OF_MONTH)} ${ALGERIAN_MONTHS[date.get(Calendar.MONTH)]}"

    private fun formatDayMonthYear(date: Calendar): String =
        "${date.get(Calendar.DAY_OF_MONTH)} ${ALGERIAN_MONTHS[date.get(Calendar.MONTH)]} ${date.get(Calendar.YEAR)}"
}
