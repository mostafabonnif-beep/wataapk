package com.elwataniatv.app

import com.elwataniatv.app.ui.screens.archive.normalizeArabicSearchText
import com.elwataniatv.app.ui.screens.archive.resumePositionMs
import com.elwataniatv.app.data.local.WatchHistoryItem
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

    @Test
    fun resumesOnlyMeaningfulUnfinishedHistory() {
        val history = listOf(
            WatchHistoryItem("finished", "Finished", "News", "https://youtu.be/finished", 95_000L, 100_000L),
            WatchHistoryItem("short", "Short", "News", "https://youtu.be/short", 5_000L, 100_000L),
            WatchHistoryItem("active", "Active", "News", "https://youtu.be/active", 30_000L, 100_000L)
        )

        assertEquals(0L, resumePositionMs(history, "finished"))
        assertEquals(0L, resumePositionMs(history, "short"))
        assertEquals(30_000L, resumePositionMs(history, "active"))
        assertEquals(0L, resumePositionMs(history, "missing"))
    }
}
