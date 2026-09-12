package com.elwataniatv.app

import com.elwataniatv.app.ui.screens.archive.archiveVideoType
import com.elwataniatv.app.ui.screens.archive.isValidVideoUrl
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

    @Test
    fun acceptsYouTubeAndDirectMediaUrls() {
        assertTrue(isValidVideoUrl("https://youtu.be/abc123"))
        assertTrue(isValidVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/clip.mp4"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/live/stream.m3u8?token=x"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/video.WEBM"))
    }

    @Test
    fun rejectsNonVideoUrls() {
        org.junit.Assert.assertFalse(isValidVideoUrl("https://example.com/page"))
        org.junit.Assert.assertFalse(isValidVideoUrl("ftp://example.com/video.mp4"))
        org.junit.Assert.assertFalse(isValidVideoUrl("not a url"))
        org.junit.Assert.assertFalse(isValidVideoUrl(""))
    }

    @Test
    fun archiveVideoTypeRoutesEngines() {
        assertEquals("youtube", archiveVideoType(" https://youtu.be/abc123 "))
        assertEquals("mp4", archiveVideoType("https://cdn.example.com/clip.mp4"))
        assertEquals("mp4", archiveVideoType("https://cdn.example.com/live/stream.m3u8"))
    }
}
