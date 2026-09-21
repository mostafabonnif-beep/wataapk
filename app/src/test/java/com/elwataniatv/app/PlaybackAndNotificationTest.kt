package com.elwataniatv.app

import com.elwataniatv.app.notifications.ReminderScheduler
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAndNotificationTest {

    private fun isVersionLessThan(current: String, required: String): Boolean {
        if (required.isBlank()) return false
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val requiredParts = required.split(".").map { it.toIntOrNull() ?: 0 }
        val size = maxOf(currentParts.size, requiredParts.size)
        for (index in 0 until size) {
            val currentPart = currentParts.getOrElse(index) { 0 }
            val requiredPart = requiredParts.getOrElse(index) { 0 }
            if (currentPart != requiredPart) return currentPart < requiredPart
        }
        return false
    }

    private fun isUpdateAvailable(
        currentCode: Int,
        currentVersionName: String,
        targetVersion: String
    ): Boolean {
        if (targetVersion.isBlank()) return false
        val trimmed = targetVersion.trim()
        val targetCode = trimmed.toIntOrNull()
        if (targetCode != null) {
            return currentCode < targetCode
        }
        return isVersionLessThan(currentVersionName, trimmed)
    }

    private fun calculateExponentialBackoffMs(retryCount: Int): Long {
        val boundedCount = retryCount.coerceAtMost(5)
        return 1000L * (1 shl (boundedCount - 1))
    }

    @Test
    fun versionCheck_comparesCorrectly() {
        assertTrue(isVersionLessThan("7.1.2", "7.2.0"))
        assertTrue(isVersionLessThan("7.1.2", "8.0.0"))
        assertFalse(isVersionLessThan("7.1.2", "7.1.2"))
        assertFalse(isVersionLessThan("7.1.2", "7.1.1"))
        assertFalse(isVersionLessThan("7.1.2", ""))
    }

    @Test
    fun updateCheck_v860_handlesNumericAndSemanticVersions() {
        val currentCode = 30
        val currentVersionName = "8.6.0"

        // Older or equal versions must NOT trigger an update alert
        assertFalse(isUpdateAvailable(currentCode, currentVersionName, "8.1.7"))
        assertFalse(isUpdateAvailable(currentCode, currentVersionName, "8.6.0"))
        assertFalse(isUpdateAvailable(currentCode, currentVersionName, "30"))
        assertFalse(isUpdateAvailable(currentCode, currentVersionName, "24"))
        assertFalse(isUpdateAvailable(currentCode, currentVersionName, ""))

        // Newer versions must trigger an update alert
        assertTrue(isUpdateAvailable(currentCode, currentVersionName, "35"))
        assertTrue(isUpdateAvailable(currentCode, currentVersionName, "8.7.0"))
        assertTrue(isUpdateAvailable(currentCode, currentVersionName, "9.0.0"))
    }

    @Test
    fun reminderTrigger_rollsPastTimesToTheNextDay() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val sameDay = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 1, 0)
        }
        val nextDay = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 2, 10, 0, 0)
        }

        assertEquals(sameDay.timeInMillis, ReminderScheduler.nextTriggerMillis("10:01", now.timeInMillis))
        assertEquals(nextDay.timeInMillis, ReminderScheduler.nextTriggerMillis("10:00", now.timeInMillis))
    }

    @Test
    fun reminderTrigger_normalizesArabicDigitsAndSeparators() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 20, 30, 0)
        }

        assertEquals(
            expected.timeInMillis,
            ReminderScheduler.nextTriggerMillis("٢٠：٣٠", now.timeInMillis)
        )
    }

    @Test
    fun reminderTrigger_doesNotTurnInvalidTimeIntoMidnight() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }

        assertEquals(
            now.timeInMillis + 24 * 60 * 60 * 1000L,
            ReminderScheduler.nextTriggerMillis("not-a-time", now.timeInMillis)
        )
    }
    @Test
    fun reminderNotifications_useDistinctPendingIntentCodes() {
        assertNotEquals(
            ReminderScheduler.requestCodeFor("morning-show"),
            ReminderScheduler.requestCodeFor("evening-news")
        )
    }


    @Test
    fun exponentialBackoff_calculatesBoundedDelays() {
        assertEquals(1000L, calculateExponentialBackoffMs(1))
        assertEquals(2000L, calculateExponentialBackoffMs(2))
        assertEquals(4000L, calculateExponentialBackoffMs(3))
        assertEquals(8000L, calculateExponentialBackoffMs(4))
    }
}
