package com.elwataniatv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun exponentialBackoff_calculatesBoundedDelays() {
        assertEquals(1000L, calculateExponentialBackoffMs(1))
        assertEquals(2000L, calculateExponentialBackoffMs(2))
        assertEquals(4000L, calculateExponentialBackoffMs(3))
        assertEquals(8000L, calculateExponentialBackoffMs(4))
    }
}
