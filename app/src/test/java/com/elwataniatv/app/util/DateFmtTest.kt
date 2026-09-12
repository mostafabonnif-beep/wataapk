package com.elwataniatv.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFmtTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 12)

    @Test
    fun formatsIsoDateWithAlgerianMonthNames() {
        assertEquals("25 جويلية 2026", DateFmt.fullDate("2026-07-25"))
        assertEquals("12 جانفي 2025", DateFmt.fullDate("2025-01-12"))
    }

    @Test
    fun relativeLabelsForRecentDates() {
        assertEquals("اليوم", DateFmt.smartDate("2026-09-12", today))
        assertEquals("أمس", DateFmt.smartDate("2026-09-11", today))
        assertEquals("منذ يومين", DateFmt.smartDate("2026-09-10", today))
        assertEquals("منذ 3 أيام", DateFmt.smartDate("2026-09-09", today))
        assertEquals("منذ أسبوع", DateFmt.smartDate("2026-09-05", today))
        assertEquals("منذ أسبوعين", DateFmt.smartDate("2026-08-29", today))
    }

    @Test
    fun sameYearShowsDayAndMonthOnly() {
        assertEquals("25 جويلية", DateFmt.smartDate("2026-07-25", today))
    }

    @Test
    fun otherYearsIncludeTheYear() {
        assertEquals("3 نوفمبر 2024", DateFmt.smartDate("2024-11-03", today))
    }

    @Test
    fun futureDatesFallBackToFullDate() {
        assertEquals("1 أكتوبر 2026", DateFmt.smartDate("2026-10-01", today))
    }

    @Test
    fun parsesIsoDatetimeWithOffset() {
        assertEquals("12 سبتمبر 2026", DateFmt.fullDate("2026-09-12T10:30:00Z"))
        assertEquals("12 سبتمبر 2026", DateFmt.fullDate("2026-09-12T10:30:00+01:00"))
    }

    @Test
    fun parsesEpochMillis() {
        assertEquals("2026-09-12", DateFmt.parse(epochFor(2026, 9, 12))!!.toString())
    }

    @Test
    fun unparseableInputReturnsRaw() {
        assertEquals("أرشيف خاص", DateFmt.smartDate("أرشيف خاص", today))
        assertEquals("", DateFmt.smartDate("", today))
    }

    private fun epochFor(year: Int, month: Int, day: Int): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Africa/Algiers"))
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis.toString()
    }
}
