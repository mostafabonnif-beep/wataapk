package com.elwataniatv.app

import com.elwataniatv.app.ui.screens.archive.normalizeArabicSearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveSearchTest {
    @Test
    fun normalizesArabicDiacriticsAndLetterVariants() {
        assertEquals(
            "نشره الاخبار الرئيسيه",
            normalizeArabicSearchText("نَشْرَةُ الأَخْبَارِ الرَّئِيسِيَّةِ")
        )
    }

    @Test
    fun normalizedQueryMatchesMixedArabicText() {
        val title = normalizeArabicSearchText("نشرة الأخبار الرئيسية")
        val query = normalizeArabicSearchText("الاخبار الرئيسيه")
        assertTrue(title.contains(query))
    }
}
