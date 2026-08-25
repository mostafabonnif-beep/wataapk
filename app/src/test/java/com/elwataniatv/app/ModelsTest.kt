package com.elwataniatv.app

import com.elwataniatv.app.data.model.AdBanner
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.model.BreakingNews
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.model.RemoteAppConfig
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.nextFallbackStream
import com.elwataniatv.app.data.model.WebsiteItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun remoteStream_defaultValues_areCorrect() {
        val stream = RemoteStream(
            id = "s1",
            title = "البث المباشر الرئيسي",
            url = "https://example.com/stream.m3u8",
            type = "m3u8"
        )

        assertEquals("s1", stream.id)
        assertEquals("البث المباشر الرئيسي", stream.title)
        assertTrue(stream.isActive)
        assertEquals("m3u8", stream.type)
    }

    @Test
    fun nextFallbackStream_usesOnlyLaterActiveConfiguredStreams() {
        val streams = listOf(
            RemoteStream("primary", "الرئيسي", "https://primary.example/live.m3u8"),
            RemoteStream("inactive", "متوقف", "https://inactive.example/live.m3u8", isActive = false),
            RemoteStream("backup", "البديل", "https://backup.example/live.m3u8"),
            RemoteStream("blank", "بدون رابط", ""),
        )

        assertEquals("backup", nextFallbackStream("primary", streams)?.id)
        assertEquals(null, nextFallbackStream("backup", streams))
        assertEquals(null, nextFallbackStream("primary", streams.filter { it.id != "backup" }))
    }

    @Test
    fun breakingNews_enabled_state() {
        val breaking = BreakingNews(enabled = true, text = "خبر عاجل هام جداً")
        assertTrue(breaking.enabled)
        assertEquals("خبر عاجل هام جداً", breaking.text)

        val disabled = BreakingNews(enabled = false, text = "")
        assertFalse(disabled.enabled)
    }

    @Test
    fun epgItem_properties_match() {
        val item = EpgItem(
            id = "e1",
            startTime = "20:00",
            title = "النشرة الرئيسية",
            category = "أخبار",
            duration = "45 دقيقة",
            description = "تغطية شاملة للأحداث الوطنية"
        )

        assertEquals("20:00", item.startTime)
        assertEquals("النشرة الرئيسية", item.title)
        assertEquals("أخبار", item.category)
    }

    @Test
    fun archiveProgram_filter_matchesCategory() {
        val list = listOf(
            ArchiveProgram(id = "a1", title = "برنامج إخباري", description = "وصف", category = "أخبار", youtubeUrl = "https://youtu.be/xxx", date = "2026-08-01"),
            ArchiveProgram(id = "a2", title = "برنامج ثفافي", description = "وصف", category = "ثقافة", youtubeUrl = "https://youtu.be/yyy", date = "2026-08-01"),
            ArchiveProgram(id = "a3", title = "حوار رياضي", description = "وصف", category = "رياضة", youtubeUrl = "https://youtu.be/zzz", date = "2026-08-01")
        )

        val newsOnly = list.filter { it.category == "أخبار" }
        assertEquals(1, newsOnly.size)
        assertEquals("a1", newsOnly.first().id)
    }

    @Test
    fun adBanner_activeFilter() {
        val banners = listOf(
            AdBanner(id = "b1", title = "راع رئيسي", imageUrl = "", targetUrl = "", isEnabled = true),
            AdBanner(id = "b2", title = "بنر غير مفعّل", imageUrl = "", targetUrl = "", isEnabled = false)
        )

        val active = banners.filter { it.isEnabled }
        assertEquals(1, active.size)
        assertEquals("b1", active.first().id)
    }

    @Test
    fun remoteAppConfig_defaultState() {
        val config = RemoteAppConfig()
        assertEquals("", config.appName)
        assertFalse(config.maintenanceMode)
        assertTrue(config.enableOnboarding)
        assertTrue(config.enableEpg)
    }
}
