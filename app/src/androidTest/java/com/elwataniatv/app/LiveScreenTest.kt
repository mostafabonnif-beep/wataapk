package com.elwataniatv.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.model.BreakingNews
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.StreamHealthState
import com.elwataniatv.app.data.remote.SyncStatus
import com.elwataniatv.app.ui.screens.LiveScreen
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setLiveContent(
        archivePrograms: List<ArchiveProgram> = emptyList(),
        breaking: BreakingNews = BreakingNews(),
        epgList: List<EpgItem> = emptyList(),
        onOpenYouTube: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            ElwataniaTVTheme {
                LiveScreen(
                    streams = emptyList(),
                    selectedStream = null,
                    breaking = breaking,
                    epgList = epgList,
                    archivePrograms = archivePrograms,
                    reminders = emptyList(),
                    onSelectStream = {},
                    onToggleReminder = { _, _ -> },
                    onOpenYouTube = onOpenYouTube
                )
            }
        }
    }

    @Test
    fun enabledBreakingNewsRendersTicker() {
        setLiveContent(breaking = BreakingNews(enabled = true, text = "خبر عاجل"))
        composeTestRule.onNodeWithTag("breaking_ticker").assertExists()
    }

    @Test
    fun tappingArchiveNewsOpensItsYouTubeUrl() {
        val videoUrl = "https://www.youtube.com/watch?v=test-news"
        var openedUrl = ""
        setLiveContent(
            archivePrograms = listOf(
                ArchiveProgram(
                    id = "news-test",
                    title = "فيديو الخبر التجريبي",
                    youtubeUrl = videoUrl,
                    category = "أخبار",
                    date = "اليوم"
                )
            ),
            onOpenYouTube = { openedUrl = it }
        )

        composeTestRule.onNodeWithText("فيديو الخبر التجريبي").performClick()
        assertEquals(videoUrl, openedUrl)
    }

    @Test
    fun tappingBreakingTickerOpensLatestArchiveVideo() {
        val videoUrl = "https://youtu.be/test-breaking"
        var openedUrl = ""
        setLiveContent(
            breaking = BreakingNews(enabled = true, text = "خبر عاجل"),
            archivePrograms = listOf(
                ArchiveProgram(
                    id = "breaking-test",
                    title = "خبر عاجل مصور",
                    youtubeUrl = videoUrl
                )
            ),
            onOpenYouTube = { openedUrl = it }
        )

        composeTestRule.onNodeWithTag("breaking_ticker").performClick()
        assertEquals(videoUrl, openedUrl)
    }

    @Test
    fun epgNextProgramCardIsVisible() {
        setLiveContent(epgList = listOf(EpgItem(id = "epg-test", startTime = "23:59", title = "برنامج تجريبي")))
        composeTestRule.onNodeWithTag("home_next_program").assertExists()
    }

    @Test
    fun internalConnectionStatusCardsAreNotRendered() {
        setLiveContent()
        composeTestRule.onNodeWithText("Connected — content from Firebase").assertDoesNotExist()
        composeTestRule.onNodeWithText("متصل — المحتوى من Firebase").assertDoesNotExist()
        composeTestRule.onNodeWithText("Stream source responding").assertDoesNotExist()
        composeTestRule.onNodeWithText("مصدر البث يستجيب").assertDoesNotExist()
    }

    @Test
    fun channelSwitcherRendersThreeConfiguredChannels() {
        val streams = listOf(
            RemoteStream("live_main", "الوطنية TV", "https://example.com/main.m3u8"),
            RemoteStream("eldjadida", "الجزائرية الجديدة", "https://example.com/news.m3u8"),
            RemoteStream("sport", "الوطنية سبورت", "https://example.com/sport.m3u8")
        )
        composeTestRule.setContent {
            ElwataniaTVTheme {
                LiveScreen(
                    streams = streams,
                    selectedStream = streams.first(),
                    breaking = BreakingNews(),
                    epgList = emptyList(),
                    reminders = emptyList(),
                    streamHealthState = StreamHealthState(isLiveActive = true),
                    syncStatus = SyncStatus(isConnected = true, lastUpdatedAt = System.currentTimeMillis()),
                    onSelectStream = {},
                    onToggleReminder = { _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("الوطنية TV").assertExists()
        composeTestRule.onNodeWithText("الجزائرية الجديدة").assertExists()
        composeTestRule.onNodeWithText("الوطنية سبورت").assertExists()
    }

    @Test
    fun streamRendersVideoPlayerTag() {
        val stream = RemoteStream(id = "test", title = "Test", url = "https://example.com/live.m3u8")
        composeTestRule.setContent {
            ElwataniaTVTheme {
                LiveScreen(
                    streams = listOf(stream),
                    selectedStream = stream,
                    breaking = BreakingNews(),
                    epgList = emptyList(),
                    reminders = emptyList(),
                    onSelectStream = {},
                    onToggleReminder = { _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithTag("live_video_player").assertExists()
    }
}
