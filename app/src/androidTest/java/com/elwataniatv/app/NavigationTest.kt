package com.elwataniatv.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elwataniatv.app.ui.components.AppBottomBar
import com.elwataniatv.app.ui.components.AppNavHost
import com.elwataniatv.app.ui.navigation.Screen
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mainItems = listOf(
        Screen.Live,
        Screen.Archive,
        Screen.Social,
        Screen.Websites,
        Screen.More
    )

    @Test
    fun productionRouteSetStartsOnLive() {
        composeTestRule.setContent { ProductionNavigationContent() }
        composeTestRule.onNodeWithTag("route_live").assertExists()
        composeTestRule.onNodeWithTag("route_archive").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_bar_live").assertExists()
        composeTestRule.onNodeWithTag("bottom_bar_archive").assertExists()
        composeTestRule.onNodeWithTag("bottom_bar_social").assertExists()
        composeTestRule.onNodeWithTag("bottom_bar_websites").assertExists()
        composeTestRule.onNodeWithTag("bottom_bar_more").assertExists()
    }

    @Test
    fun bottomBarArchiveItemNavigatesToArchiveRoute() {
        composeTestRule.setContent { ProductionNavigationContent() }
        composeTestRule.onNodeWithTag("bottom_bar_archive").performClick()
        composeTestRule.onNodeWithTag("route_archive").assertExists()
        composeTestRule.onNodeWithTag("route_live").assertDoesNotExist()
    }

    @Test
    fun bottomBarMoreItemNavigatesToMoreRoute() {
        composeTestRule.setContent { ProductionNavigationContent() }
        composeTestRule.onNodeWithTag("bottom_bar_more").performClick()
        composeTestRule.onNodeWithTag("route_more").assertExists()
    }

    @Composable
    private fun ProductionNavigationContent() {
        val navController = rememberNavController()
        ElwataniaTVTheme {
            Column(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    startDestination = Screen.Live.route,
                    modifier = Modifier.weight(1f)
                ) {
                    allTestScreens().forEach { screen ->
                        composable(screen.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("route_${screen.route}")
                            ) {
                                Text(screen.route)
                            }
                        }
                    }
                }
                AppBottomBar(
                    items = mainItems,
                    currentRoute = navController.currentBackStackEntry?.destination?.route,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private fun allTestScreens(): List<Screen> = listOf(
    Screen.Live,
    Screen.Archive,
    Screen.Social,
    Screen.Websites,
    Screen.More,
    Screen.Favorites,
    Screen.History,
    Screen.Settings
)
