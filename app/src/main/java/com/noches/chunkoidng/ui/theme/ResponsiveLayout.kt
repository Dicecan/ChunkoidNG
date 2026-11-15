package com.noches.chunkoidng.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WindowWidthTier {
    COMPACT_NARROW,
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class AdaptiveMetrics(
    val widthTier: WindowWidthTier,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val fontScale: Float,
    val horizontalPadding: Dp,
    val gridColumns: Int,
    val maxContentWidth: Dp,
    val dialogWidthFraction: Float,
    val dialogHeightFraction: Float,
    val dialogMaxWidth: Dp,
    val dialogMaxHeight: Dp,
    val isCompactHeight: Boolean,
    val isLargeFontScale: Boolean,
    val navLabelFontSize: TextUnit
)

@Composable
fun rememberAdaptiveMetrics(): AdaptiveMetrics {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    val fontScale = density.fontScale

    val isLargeFontScale = fontScale >= 1.15f
    val tier = when {
        widthDp < 360 || (widthDp < 410 && fontScale >= 1.3f) -> WindowWidthTier.COMPACT_NARROW
        widthDp < 600 -> WindowWidthTier.COMPACT
        widthDp < 840 -> WindowWidthTier.MEDIUM
        else -> WindowWidthTier.EXPANDED
    }

    val columns = when (tier) {
        WindowWidthTier.COMPACT_NARROW -> 1
        WindowWidthTier.COMPACT -> 2
        WindowWidthTier.MEDIUM -> 3
        WindowWidthTier.EXPANDED -> 4
    }

    val hPadding = when (tier) {
        WindowWidthTier.COMPACT_NARROW -> 12.dp
        WindowWidthTier.COMPACT -> 16.dp
        WindowWidthTier.MEDIUM -> 24.dp
        WindowWidthTier.EXPANDED -> 32.dp
    }

    val navLabelSp = when {
        widthDp < 360 || fontScale >= 1.2f -> 9.5.sp
        widthDp < 410 || fontScale >= 1.1f -> 10.sp
        else -> 11.5.sp
    }

    return AdaptiveMetrics(
        widthTier = tier,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        fontScale = fontScale,
        horizontalPadding = hPadding,
        gridColumns = columns,
        maxContentWidth = if (tier == WindowWidthTier.EXPANDED) 960.dp else 760.dp,
        dialogWidthFraction = when (tier) {
            WindowWidthTier.COMPACT_NARROW -> 0.96f
            WindowWidthTier.COMPACT -> 0.94f
            WindowWidthTier.MEDIUM -> 0.84f
            WindowWidthTier.EXPANDED -> 0.72f
        },
        dialogHeightFraction = when {
            heightDp < 540 -> 0.95f
            heightDp < 680 -> 0.90f
            else -> 0.84f
        },
        dialogMaxWidth = 580.dp,
        dialogMaxHeight = 760.dp,
        isCompactHeight = heightDp < 600,
        isLargeFontScale = isLargeFontScale,
        navLabelFontSize = navLabelSp
    )
}
