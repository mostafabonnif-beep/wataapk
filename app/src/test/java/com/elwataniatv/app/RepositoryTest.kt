package com.elwataniatv.app

import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.model.featuredArchivePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {

    private val sampleArchive = listOf(
        ArchiveProgram(id = "1", title = "النشرة الإخبارية الرئيسية", description = "أخبار الجزائر اليومية", category = "أخبار", youtubeUrl = "https://youtu.be/111", date = "2026-08-01"),
        ArchiveProgram(id = "2", title = "اللقاء الرياضي الأسبوعي", description = "تحليل المباريات الوطنية", category = "رياضة", youtubeUrl = "https://youtu.be/222", date = "2026-08-01"),
        ArchiveProgram(id = "3", title = "أضواء على الاقتصاد", description = "مستجدات الأسواق والاستثمار", category = "اقتصاد", youtubeUrl = "https://youtu.be/333", date = "2026-08-01"),
        ArchiveProgram(id = "4", title = "حوار الجمعة الإخباري", description = "مقابلات حصرية", category = "أخبار", youtubeUrl = "https://youtu.be/444", date = "2026-08-01")
    )

    @Test
    fun archiveFiltering_byCategory_returnsCorrectItems() {
        val newsItems = sampleArchive.filter { it.category == "أخبار" }
        assertEquals(2, newsItems.size)
        assertTrue(newsItems.all { it.category == "أخبار" })
    }

    @Test
    fun archiveFiltering_allCategory_returnsAllItems() {
        val selectedCat = "الكل"
        val filtered = if (selectedCat == "الكل") sampleArchive else sampleArchive.filter { it.category == selectedCat }
        assertEquals(4, filtered.size)
    }

    @Test
    fun archiveSearching_byTitleQuery_returnsMatches() {
        val query = "الرياضي"
        val matches = sampleArchive.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
        assertEquals(1, matches.size)
        assertEquals("2", matches.first().id)
    }

    @Test
    fun featuredArchivePreview_prefersEditoriallySelectedItems() {
        val programs = listOf(
            sampleArchive[0].copy(isFeatured = false),
            sampleArchive[1].copy(isFeatured = true),
            sampleArchive[2].copy(isFeatured = false)
        )

        val preview = featuredArchivePreview(programs, limit = 2)

        assertEquals(listOf("2"), preview.map { it.id })
    }

    @Test
    fun featuredArchivePreview_fallsBackToArchiveWhenNoneSelected() {
        val programs = sampleArchive.map { it.copy(isFeatured = false) }

        val preview = featuredArchivePreview(programs, limit = 2)

        assertEquals(listOf("1", "2"), preview.map { it.id })
    }

    @Test
    fun archiveSearching_emptyQuery_returnsFullList() {
        val query = ""
        val matches = sampleArchive.filter {
            query.isBlank() || it.title.contains(query, ignoreCase = true)
        }
        assertEquals(4, matches.size)
    }
}
