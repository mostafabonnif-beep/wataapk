package com.elwataniatv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.ui.screens.archive.ArchiveScreen
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val programs = listOf(
        ArchiveProgram(
            id = "news",
            title = "النشرة الرئيسية",
            youtubeUrl = "https://youtube.com/watch?v=news",
            category = "أخبار"
        ),
        ArchiveProgram(
            id = "sport",
            title = "الملف الرياضي",
            youtubeUrl = "https://youtube.com/watch?v=sport",
            category = "رياضة"
        )
    )

    @Test
    fun nonEmptyProgramsAreDisplayed() {
        composeTestRule.setContent { ArchiveContent() }
        composeTestRule.onNodeWithText("النشرة الرئيسية").assertExists()
        composeTestRule.onNodeWithText("الملف الرياضي").assertExists()
    }

    @Test
    fun categoryFilterHidesOtherCategories() {
        composeTestRule.setContent { ArchiveContent() }
        composeTestRule.onNodeWithText("أخبار").performClick()
        composeTestRule.onNodeWithText("النشرة الرئيسية").assertExists()
        composeTestRule.onNodeWithText("الملف الرياضي").assertDoesNotExist()
    }

    @Test
    fun searchFiltersPrograms() {
        composeTestRule.setContent { ArchiveContent() }
        composeTestRule.onNodeWithTag("archive_search_input").performTextInput("رياضي")
        composeTestRule.onNodeWithText("الملف الرياضي").assertExists()
        composeTestRule.onNodeWithText("النشرة الرئيسية").assertDoesNotExist()
    }

    @Composable
    private fun ArchiveContent() {
        var selectedCategory by remember { mutableStateOf("الكل") }
        var searchQuery by remember { mutableStateOf("") }
        ElwataniaTVTheme {
            ArchiveScreen(
                programs = programs.filter { program ->
                    (selectedCategory == "الكل" || program.category == selectedCategory) &&
                        (searchQuery.isBlank() || program.title.contains(searchQuery))
                },
                favorites = emptyList(),
                selectedCategory = selectedCategory,
                searchQuery = searchQuery,
                onSelectCategory = { selectedCategory = it },
                onSearchQueryChange = { searchQuery = it },
                onToggleFavorite = { _, _ -> }
            )
        }
    }
}
