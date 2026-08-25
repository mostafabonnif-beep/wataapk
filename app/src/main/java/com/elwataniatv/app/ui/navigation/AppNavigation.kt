package com.elwataniatv.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.elwataniatv.app.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Live : Screen("live", R.string.tab_live, Icons.Default.LiveTv)
    object Archive : Screen("archive", R.string.tab_archive, Icons.Default.OndemandVideo)
    object Favorites : Screen("favorites", R.string.tab_favorites, Icons.Default.Bookmark)
    object Websites : Screen("websites", R.string.tab_websites, Icons.Default.Language)
    object More : Screen("more", R.string.tab_more, Icons.Default.MoreHoriz)

    // Secondary routes accessible from More screen / Live hero
    object History : Screen("history", R.string.tab_history, Icons.Default.History)
    object Social : Screen("social", R.string.tab_social, Icons.Default.People)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)
    object Guide : Screen("guide", R.string.epg_today, Icons.Default.CalendarMonth)
}

