package com.example.scpd_tracker.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing tokens. Names match the Figma `Spacing` variables (space-1 = 4dp ... space-12 = 48dp). */
@Immutable
data class ScpdSpacing(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space7: Dp = 28.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
) {
    /** Horizontal screen margin on compact widths. */
    val gutter: Dp get() = space5

    /** Minimum touch target (WCAG 2.5.5 / Material). */
    val minTouchTarget: Dp get() = space12

    /** Minimum height of filled and outlined buttons (Figma spec 5:466). */
    val buttonHeight: Dp get() = 52.dp

    /** Widest a reading column grows on tablets and in landscape. */
    val maxContentWidth: Dp get() = 640.dp
}

val LocalSpacing = staticCompositionLocalOf { ScpdSpacing() }

/** Radius tokens: sm 8, md 12, lg 20, xl 24, full = pill. */
val ScpdShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val PillShape = CircleShape
