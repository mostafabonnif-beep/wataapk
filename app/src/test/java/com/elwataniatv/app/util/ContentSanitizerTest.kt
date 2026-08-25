package com.elwataniatv.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSanitizerTest {
    @Test
    fun rejectsKnownPlaceholderValuesWithSeparators() {
        assertTrue(ContentSanitizer.isKnownTestValue("  Oussama  "))
        assertTrue(ContentSanitizer.isKnownTestValue("2-0-0"))
        assertTrue(ContentSanitizer.isKnownTestValue("oussama!!!"))
    }

    @Test
    fun rejectsKnownTestNameVariants() {
        assertTrue(ContentSanitizer.isKnownTestValue("oussama_b"))
        assertFalse(ContentSanitizer.isUsable("oussama_b"))
    }

    @Test
    fun keepsRealArabicEditorialContent() {
        assertFalse(ContentSanitizer.isKnownTestValue("نشرة الأخبار الوطنية"))
        assertTrue(ContentSanitizer.isUsable("نشرة الأخبار الوطنية"))
    }

    @Test
    fun rejectsBlankContentButDoesNotRejectNormalUrls() {
        assertFalse(ContentSanitizer.isUsable("   "))
        assertTrue(ContentSanitizer.isUsable("https://www.youtube.com/@ElwataniaTV"))
    }
}
