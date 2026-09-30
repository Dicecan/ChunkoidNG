package com.noches.chunkoidng.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.noches.chunkoidng.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Features : Screen(
        route = "features",
        titleRes = R.string.nav_features,
        unselectedIcon = Icons.Outlined.GridView,
        selectedIcon = Icons.Filled.GridView
    )

    data object Tutorial : Screen(
        route = "tutorial",
        titleRes = R.string.nav_tutorial,
        unselectedIcon = Icons.Outlined.AutoStories,
        selectedIcon = Icons.Filled.AutoStories
    )

    data object Settings : Screen(
        route = "settings",
        titleRes = R.string.nav_settings,
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )

    data object About : Screen(
        route = "about",
        titleRes = R.string.nav_about,
        unselectedIcon = Icons.Outlined.Info,
        selectedIcon = Icons.Filled.Info
    )

    companion object {
        val topLevelScreens: List<Screen>
            get() = listOf(Features, Tutorial, Settings, About)
    }
}
