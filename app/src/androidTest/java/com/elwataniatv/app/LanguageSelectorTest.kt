package com.elwataniatv.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.ui.screens.settings.AppearancePreferencesCard
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingEnglishEmitsEnglishLanguageCode() {
        var selectedLanguage = "ar"
        composeRule.setContent {
            ElwataniaTVTheme {
                AppearancePreferencesCard(
                    darkModeEnabled = true,
                    pushNotificationsEnabled = true,
                    selectedLanguage = "العربية",
                    onDarkModeChange = {},
                    onPushChange = {},
                    onUpdatePreferences = { _, _ -> },
                    onLanguageChange = { selectedLanguage = it }
                )
            }
        }

        composeRule.onNodeWithTag("language_selector").performClick()
        composeRule.onNodeWithTag("language_option_en").performClick()
        assertEquals("en", selectedLanguage)
    }
}
