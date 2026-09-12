package com.noches.chunkoidng.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Specific MD3 Expressive Component Shapes
val SquircleIconShape = RoundedCornerShape(16.dp)
val PillShape = CircleShape
val StaggeredCardShape = RoundedCornerShape(22.dp)
val BottomNavPillShape = RoundedCornerShape(20.dp)
val ChipBadgeShape = RoundedCornerShape(10.dp)
