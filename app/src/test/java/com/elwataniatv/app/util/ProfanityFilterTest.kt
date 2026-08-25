package com.elwataniatv.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirrors functions/test/profanity.test.js — keep both in sync so the
 * client filter and the (future) server function behave identically.
 */
class ProfanityFilterTest {

    @Test
    fun isProfaneDetectsBannedWordsAndIgnoresNormalText() {
        assertTrue(ProfanityFilter.isProfane("This is a fuck sentence"))
        assertFalse(ProfanityFilter.isProfane("هذا تعليق مفيد عن البرنامج"))
        assertFalse(ProfanityFilter.isProfane(""))
    }

    @Test
    fun checkAcceptsANormalComment() {
        val result = ProfanityFilter.check("شكراً على هذا البرنامج")
        assertTrue(result.ok)
        assertEquals(null, result.reason)
    }

    @Test
    fun checkReportsTheMainRejectionReasons() {
        assertEquals(ProfanityFilter.REASON_TOO_SHORT, ProfanityFilter.check("").reason)
        assertEquals(ProfanityFilter.REASON_PROFANE, ProfanityFilter.check("fuck").reason)
        assertEquals(
            ProfanityFilter.REASON_TOO_MANY_LINKS,
            ProfanityFilter.check("https://a.test https://b.test https://c.test").reason
        )
        assertEquals(ProfanityFilter.REASON_SHOUTING, ProfanityFilter.check("A".repeat(21)).reason)
        assertEquals(ProfanityFilter.REASON_SPAM_REPEAT, ProfanityFilter.check("x".repeat(8)).reason)
        assertEquals(ProfanityFilter.REASON_TOO_LONG, ProfanityFilter.check("a".repeat(501)).reason)
    }

    @Test
    fun checkAllowsUpToTwoLinks() {
        // profanity.js only flags >= 3 links; the Firestore rules reject
        // > 1 link server-side, so 2-link comments pass here and are
        // blocked by the rules (documented divergence).
        assertTrue(ProfanityFilter.check("انظر https://elwataniatv.dz و https://youtube.com").ok)
    }

    @Test
    fun userMessageCoversReasons() {
        assertTrue(ProfanityFilter.userMessage(ProfanityFilter.REASON_PROFANE).isNotBlank())
        assertTrue(ProfanityFilter.userMessage(ProfanityFilter.REASON_TOO_LONG).isNotBlank())
        assertTrue(ProfanityFilter.userMessage(null).isNotBlank())
    }
}
