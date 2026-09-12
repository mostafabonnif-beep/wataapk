package com.elwataniatv.app

import com.elwataniatv.app.ui.screens.archive.archiveVideoType
import com.elwataniatv.app.ui.screens.archive.isValidVideoUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveVideoUrlTest {
    @Test
    fun acceptsYouTubeLinks() {
        assertTrue(isValidVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertTrue(isValidVideoUrl("https://youtu.be/abc123"))
    }

    @Test
    fun acceptsDirectProgressiveAndHlsLinks() {
        assertTrue(isValidVideoUrl("https://cdn.example.com/news/bulletin.mp4"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/live/stream/index.m3u8?token=x"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/clip.webm"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/recording.MOV"))
        assertTrue(isValidVideoUrl("https://cdn.example.com/segment.ts#t=10"))
    }

    @Test
    fun rejectsUnsupportedLinks() {
        assertFalse(isValidVideoUrl("https://cdn.example.com/bulletin.mp3"))
        assertFalse(isValidVideoUrl("https://cdn.example.com/page"))
        assertFalse(isValidVideoUrl("ftp://cdn.example.com/bulletin.mp4"))
        assertFalse(isValidVideoUrl(""))
        assertFalse(isValidVideoUrl("   "))
    }

    @Test
    fun picksEngineTypePerSource() {
        assertEquals("youtube", archiveVideoType("https://youtu.be/abc123"))
        assertEquals("mp4", archiveVideoType("https://cdn.example.com/bulletin.mp4"))
        assertEquals("mp4", archiveVideoType("https://cdn.example.com/live/index.m3u8"))
    }
}
