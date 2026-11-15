package com.noches.chunkoidng.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.ui.navigation.Screen
import com.noches.chunkoidng.ui.theme.WindowWidthTier
import com.noches.chunkoidng.ui.theme.rememberAdaptiveMetrics

@Composable
fun ChunkoidNavigationBar(
    currentScreen: Screen,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = rememberAdaptiveMetrics()
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Screen.topLevelScreens.forEach { screen ->
            val selected = currentScreen == screen
            val title = stringResource(screen.titleRes)
            val labelFontSize = if (title.length >= 6 && metrics.widthTier <= WindowWidthTier.COMPACT) {
                9.5.sp
            } else {
                metrics.navLabelFontSize
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateTo(screen) },
                alwaysShowLabel = true,
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = title
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontSize = labelFontSize,
                        letterSpacing = if (title.length >= 6) (-0.3).sp else 0.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
