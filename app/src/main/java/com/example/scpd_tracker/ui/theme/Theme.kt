package com.example.scpd_tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The single app theme. Dynamic colour is deliberately off so the university brand colours
 * stay fixed on Android 12+ devices (including the Pixel 2 test emulator).
 */
@Composable
fun ScpdTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides ScpdSpacing(),
        LocalScpdColors provides ScpdExtendedColors(),
        LocalPosterType provides ScpdPosterType(),
    ) {
        MaterialTheme(
            colorScheme = ScpdLightColors,
            typography = ScpdTypography,
            shapes = ScpdShapes,
            content = content,
        )
    }
}

/** Accessors for the tokens that Material 3 does not cover. */
object ScpdTheme {
    val spacing: ScpdSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current

    val colors: ScpdExtendedColors
        @Composable @ReadOnlyComposable get() = LocalScpdColors.current

    val poster: ScpdPosterType
        @Composable @ReadOnlyComposable get() = LocalPosterType.current
}
