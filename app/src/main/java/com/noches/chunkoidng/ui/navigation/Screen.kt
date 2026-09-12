package com.noches.chunkoidng.ui.navigation

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

sealed class Screen(
    val route: String,
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Features : Screen(
        route = "features",
        title = "功能",
        unselectedIcon = Icons.Outlined.GridView,
        selectedIcon = Icons.Filled.GridView
    )

    data object Tutorial : Screen(
        route = "tutorial",
        title = "教程",
        unselectedIcon = Icons.Outlined.AutoStories,
        selectedIcon = Icons.Filled.AutoStories
    )

    data object Settings : Screen(
        route = "settings",
        title = "设置",
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )

    data object About : Screen(
        route = "about",
        title = "关于",
        unselectedIcon = Icons.Outlined.Info,
        selectedIcon = Icons.Filled.Info
    )

    companion object {
        val topLevelScreens: List<Screen>
            get() = listOf(Features, Tutorial, Settings, About)
    }
}
