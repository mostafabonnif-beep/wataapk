package com.elwataniatv.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.ui.components.AppBottomBar
import com.elwataniatv.app.ui.navigation.Screen
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class AppBottomBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = listOf(
        Screen.Live,
        Screen.Archive,
        Screen.Social,
        Screen.Websites,
        Screen.More
    )

    @Test
    fun compactRtl_rendersEveryNavigationItem() {
        composeTestRule.setContent {
            FixedViewport(width = 320.dp, height = 568.dp) {
                RtlContent {
                    AppBottomBar(
                        items = items,
                        currentRoute = Screen.Live.route,
                        onNavigate = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("app_bottom_bar").assertIsDisplayed()
        items.forEach { screen ->
            composeTestRule.onNodeWithTag("bottom_bar_${screen.route}").assertIsDisplayed()
        }
    }

    @Test
    fun tabletRtl_rendersExpandedNavigationItem() {
        composeTestRule.setContent {
            FixedViewport(width = 1024.dp, height = 600.dp) {
                RtlContent {
                    AppBottomBar(
                        items = items,
                        currentRoute = Screen.Archive.route,
                        onNavigate = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("app_bottom_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_bar_archive").assertIsDisplayed()
    }

    @Test
    fun rtlClickReportsTheLogicalScreen() {
        var clicked: Screen? = null
        composeTestRule.setContent {
            RtlContent {
                AppBottomBar(
                    items = items,
                    currentRoute = Screen.Live.route,
                    onNavigate = { clicked = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_bar_more").performClick()
        assertEquals(Screen.More, clicked)
    }

    @Composable
    private fun RtlContent(content: @Composable () -> Unit) {
        ElwataniaTVTheme {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                content()
            }
        }
    }

    @Composable
    private fun FixedViewport(width: Dp, height: Dp, content: @Composable () -> Unit) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .testTag("fixed_viewport")
        ) {
            content()
        }
    }
}
