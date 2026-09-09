package com.elwataniatv.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlTest {
    @Test
    fun extractsVideoIdsFromSupportedUrlForms() {
        assertEquals("abc123XYZ_1", extractYouTubeVideoId("https://www.youtube.com/watch?v=abc123XYZ_1"))
        assertEquals("abc123XYZ_1", extractYouTubeVideoId("https://youtu.be/abc123XYZ_1?t=12"))
        assertEquals("abc123XYZ_1", extractYouTubeVideoId("https://www.youtube.com/shorts/abc123XYZ_1"))
        assertEquals("abc123XYZ_1", extractYouTubeVideoId("https://www.youtube.com/live/abc123XYZ_1"))
    }

    @Test
    fun rejectsNonVideoPagesAndUnsafeSchemes() {
        assertFalse(isYouTubeVideoUrl("https://www.youtube.com/@elwataniatv"))
        assertFalse(isYouTubeVideoUrl("javascript:alert(1)"))
        assertFalse(isYouTubeVideoUrl("https://example.com/watch?v=abc123XYZ_1"))
    }

    @Test
    fun createsPrivacyFriendlyEmbedUrl() {
        assertEquals(
            "https://www.youtube-nocookie.com/embed/abc123XYZ_1?autoplay=1&modestbranding=1&rel=0&playsinline=1",
            youtubeEmbedUrl("https://youtu.be/abc123XYZ_1")
        )
    }
}
