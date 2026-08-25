package com.elwataniatv.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.ui.components.PremiumLiveHero
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class PremiumLiveHeroTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val stream = RemoteStream(
        id = "main",
        title = "الوطنية TV",
        url = "https://example.com/live.m3u8"
    )

    @Test
    fun archiveQuickActionInvokesNavigationCallback() {
        var archiveOpened = false
        composeRule.setContent {
            ElwataniaTVTheme {
                PremiumLiveHero(
                    streams = listOf(stream),
                    selectedStream = stream,
                    onWatchLive = {},
                    onOpenGuide = {},
                    onOpenArchive = { archiveOpened = true }
                )
            }
        }

        composeRule.onNodeWithTag("hero_quick_archive").assertExists().performClick()
        assertTrue(archiveOpened)
    }

    @Test
    fun firebaseConfiguredIdentityAppearsInHero() {
        composeRule.setContent {
            ElwataniaTVTheme {
                PremiumLiveHero(
                    streams = listOf(stream),
                    selectedStream = stream,
                    appName = "الوطنية TV الإخبارية",
                    appSlogan = "البث الرسمي على مدار الساعة",
                    onWatchLive = {},
                    onOpenGuide = {},
                    onOpenArchive = {}
                )
            }
        }

        composeRule.onNodeWithText("الوطنية TV الإخبارية").assertExists()
        composeRule.onNodeWithText("البث الرسمي على مدار الساعة").assertExists()
    }

    @Test
    fun guideQuickActionInvokesGuideCallback() {
        var guideOpened = false
        composeRule.setContent {
            ElwataniaTVTheme {
                PremiumLiveHero(
                    streams = listOf(stream),
                    selectedStream = stream,
                    onWatchLive = {},
                    onOpenGuide = { guideOpened = true },
                    onOpenArchive = {}
                )
            }
        }

        composeRule.onNodeWithTag("hero_quick_guide").assertExists().performClick()
        assertTrue(guideOpened)
    }
}
