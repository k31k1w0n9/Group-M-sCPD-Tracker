package com.example.scpd_tracker.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Primitive colour tokens. These mirror the Figma `Primitives` collection from the design audit.
 * Screens should read semantic roles (MaterialTheme.colorScheme or [ScpdTheme.colors]), not these.
 */
object ScpdPalette {
    val Navy950 = Color(0xFF0B2238)
    val Navy900 = Color(0xFF10263B) // Nottingham Blue, 15.41:1 on white
    val Navy800 = Color(0xFF405162) // 8.17:1 on white
    val Navy700 = Color(0xFF536474) // text-tertiary, 6.10:1 on white, 5.54:1 on Navy100
    val Navy500 = Color(0xFF707D89) // icons and input borders only (4.21:1), never small text
    val Navy400 = Color(0xFF9FA8B1) // disabled content only
    val Navy200 = Color(0xFFCFD4D8) // dividers and decorative strokes
    val Navy100 = Color(0xFFF3F4F5) // app background
    val Stone50 = Color(0xFFFDFBF9) // Portland Stone surface
    val White = Color(0xFFFFFFFF)

    val Teal500 = Color(0xFF37B4B0) // Trent Turquoise: fills only, with Navy900 on top (6.11:1)
    val Teal700 = Color(0xFF146E6E) // teal text, 6.03:1 on white
    val Teal50 = Color(0xFFE3F4F4)

    val Gold500 = Color(0xFFDEB406) // Rebel's Gold: fills only, with Navy900 on top (7.81:1)
    val Gold800 = Color(0xFF805700) // gold text, 6.40:1 on white
    val Gold50 = Color(0xFFF9F2D2)

    val Red700 = Color(0xFFA4343A)
    val Red50 = Color(0xFFFBE9E8)
}

/** Semantic Material 3 colour scheme (Figma `Semantic` collection, Light mode). */
val ScpdLightColors = lightColorScheme(
    primary = ScpdPalette.Navy900,
    onPrimary = ScpdPalette.White,
    primaryContainer = ScpdPalette.Navy100,
    onPrimaryContainer = ScpdPalette.Navy900,
    secondary = ScpdPalette.Teal500,
    onSecondary = ScpdPalette.Navy900,
    secondaryContainer = ScpdPalette.Teal50,
    onSecondaryContainer = ScpdPalette.Teal700,
    tertiary = ScpdPalette.Gold500,
    onTertiary = ScpdPalette.Navy900,
    tertiaryContainer = ScpdPalette.Gold50,
    onTertiaryContainer = ScpdPalette.Gold800,
    error = ScpdPalette.Red700,
    onError = ScpdPalette.White,
    errorContainer = ScpdPalette.Red50,
    onErrorContainer = ScpdPalette.Red700,
    background = ScpdPalette.Navy100,
    onBackground = ScpdPalette.Navy900,
    surface = ScpdPalette.Stone50,
    onSurface = ScpdPalette.Navy900,
    surfaceVariant = ScpdPalette.Navy100,
    onSurfaceVariant = ScpdPalette.Navy800,
    surfaceContainerLowest = ScpdPalette.White,
    surfaceContainerLow = ScpdPalette.White,
    surfaceContainer = ScpdPalette.White,
    inverseSurface = ScpdPalette.Navy900, // snackbars
    inverseOnSurface = ScpdPalette.White,
    inversePrimary = ScpdPalette.Teal500,
    outline = ScpdPalette.Navy500,
    outlineVariant = ScpdPalette.Navy200,
    scrim = ScpdPalette.Navy900,
)

/** Brand roles that Material 3 has no slot for. */
@Immutable
data class ScpdExtendedColors(
    val textTertiary: Color = ScpdPalette.Navy700,
    val onHero: Color = ScpdPalette.White,
    val onHeroMuted: Color = ScpdPalette.White.copy(alpha = 0.72f),
    val heroBackground: Color = ScpdPalette.Navy900,
    val heroOverlay: Color = ScpdPalette.Navy900.copy(alpha = 0.85f),
    val posterOverlay: Color = ScpdPalette.Navy900.copy(alpha = 0.75f),
    val heroFieldContainer: Color = ScpdPalette.White.copy(alpha = 0.10f),
    val heroFieldBorder: Color = ScpdPalette.White.copy(alpha = 0.40f),
    val disabledContent: Color = ScpdPalette.Navy400,
)

val LocalScpdColors = staticCompositionLocalOf { ScpdExtendedColors() }
