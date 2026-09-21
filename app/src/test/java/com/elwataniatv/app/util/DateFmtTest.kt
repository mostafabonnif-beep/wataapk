package com.elwataniatv.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateFmtTest {

    private fun calendarFor(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers")).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }

    private val today: Calendar = calendarFor(2026, 9, 12)
    private val arLocale = Locale("ar")
    private val frLocale = Locale("fr")
    private val enLocale = Locale("en")

    @Test
    fun formatsIsoDateWithAlgerianMonthNames() {
        assertEquals("25 جويلية 2026", DateFmt.fullDate("2026-07-25", arLocale))
        assertEquals("12 جانفي 2025", DateFmt.fullDate("2025-01-12", arLocale))
        assertEquals("25 juillet 2026", DateFmt.fullDate("2026-07-25", frLocale))
        assertEquals("July 25, 2026", DateFmt.fullDate("2026-07-25", enLocale))
    }

    @Test
    fun relativeLabelsForRecentDates() {
        assertEquals("اليوم", DateFmt.smartDate("2026-09-12", today, arLocale))
        assertEquals("أمس", DateFmt.smartDate("2026-09-11", today, arLocale))
        assertEquals("منذ يومين", DateFmt.smartDate("2026-09-10", today, arLocale))
        assertEquals("منذ 3 أيام", DateFmt.smartDate("2026-09-09", today, arLocale))
        assertEquals("منذ أسبوع", DateFmt.smartDate("2026-09-05", today, arLocale))
        assertEquals("منذ أسبوعين", DateFmt.smartDate("2026-08-29", today, arLocale))

        assertEquals("Aujourd'hui", DateFmt.smartDate("2026-09-12", today, frLocale))
        assertEquals("Hier", DateFmt.smartDate("2026-09-11", today, frLocale))
        assertEquals("Today", DateFmt.smartDate("2026-09-12", today, enLocale))
        assertEquals("Yesterday", DateFmt.smartDate("2026-09-11", today, enLocale))
    }

    @Test
    fun sameYearShowsDayAndMonthOnly() {
        assertEquals("25 جويلية", DateFmt.smartDate("2026-07-25", today, arLocale))
        assertEquals("25 juillet", DateFmt.smartDate("2026-07-25", today, frLocale))
        assertEquals("July 25", DateFmt.smartDate("2026-07-25", today, enLocale))
    }

    @Test
    fun otherYearsIncludeTheYear() {
        assertEquals("3 نوفمبر 2024", DateFmt.smartDate("2024-11-03", today, arLocale))
        assertEquals("3 novembre 2024", DateFmt.smartDate("2024-11-03", today, frLocale))
        assertEquals("November 3, 2024", DateFmt.smartDate("2024-11-03", today, enLocale))
    }

    @Test
    fun futureDatesFallBackToFullDate() {
        assertEquals("1 أكتوبر 2026", DateFmt.smartDate("2026-10-01", today, arLocale))
        assertEquals("1 octobre 2026", DateFmt.smartDate("2026-10-01", today, frLocale))
        assertEquals("October 1, 2026", DateFmt.smartDate("2026-10-01", today, enLocale))
    }

    @Test
    fun parsesIsoDatetimeWithZSuffix() {
        assertEquals("12 سبتمبر 2026", DateFmt.fullDate("2026-09-12T10:30:00Z", arLocale))
    }

    @Test
    fun parsesIsoDatetimeWithOffset() {
        assertEquals("12 سبتمبر 2026", DateFmt.fullDate("2026-09-12T10:30:00+01:00", arLocale))
    }

    @Test
    fun parsesEpochMillis() {
        val parsed = DateFmt.parse(epochFor(2026, 9, 12))
        assertEquals(2026, parsed!!.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, parsed.get(Calendar.MONTH))
        assertEquals(12, parsed.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun unparseableInputReturnsRaw() {
        assertEquals("أرشيف خاص", DateFmt.smartDate("أرشيف خاص", today))
        assertEquals("", DateFmt.smartDate("", today))
    }

    @Test
    fun parseReturnsNullForGarbage() {
        assertNull(DateFmt.parse("أرشيف خاص"))
        assertNull(DateFmt.parse(""))
        assertNull(DateFmt.parse("not a date at all"))
    }

    private fun epochFor(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"))
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis.toString()
    }
}
