package com.example.scpd_tracker

import androidx.compose.ui.graphics.Color
import com.example.scpd_tracker.ui.theme.ScpdExtendedColors
import com.example.scpd_tracker.ui.theme.ScpdLightColors
import com.example.scpd_tracker.ui.theme.ScpdPalette
import com.example.scpd_tracker.ui.theme.ScpdSpacing
import com.example.scpd_tracker.ui.theme.ScpdTypography
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Guards the accessibility rules from the design audit so a token change cannot silently regress them. */
class ThemeTokensTest {
    private fun channel(v: Float) = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertAa(name: String, fg: Color, bg: Color, minimum: Double = 4.5) {
        val ratio = contrast(fg, bg)
        assertTrue("$name is %.2f:1, needs %.1f:1".format(ratio, minimum), ratio >= minimum)
    }

    private val scheme = ScpdLightColors
    private val extra = ScpdExtendedColors()

    @Test
    fun textRolesPassAaOnEverySurface() {
        listOf(scheme.background, scheme.surface, scheme.surfaceContainerLowest).forEach { surface ->
            assertAa("onSurface", scheme.onSurface, surface)
            assertAa("onSurfaceVariant", scheme.onSurfaceVariant, surface)
            assertAa("textTertiary", extra.textTertiary, surface)
        }
    }

    @Test
    fun onColourPairsPassAa() {
        assertAa("onPrimary/primary", scheme.onPrimary, scheme.primary)
        assertAa("onSecondary/secondary", scheme.onSecondary, scheme.secondary)
        assertAa("onTertiary/tertiary", scheme.onTertiary, scheme.tertiary)
        assertAa("onSecondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertAa("onTertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertAa("onErrorContainer", scheme.onErrorContainer, scheme.errorContainer)
    }

    @Test
    fun inputBordersMeetNonTextContrast() {
        assertAa("outline on white", scheme.outline, scheme.surfaceContainerLowest, minimum = 3.0)
        assertAa("outline on background", scheme.outline, scheme.background, minimum = 3.0)
    }

    @Test
    fun heroTextStaysReadableOverABrightPhoto() {
        // Worst case: the overlay sits on a pure white photo pixel.
        val overlayOnWhite = extra.heroOverlay.compositeOver(Color.White)
        assertAa("white on hero overlay", extra.onHero, overlayOnWhite)
        assertAa("muted white on hero overlay", extra.onHeroMuted.compositeOver(overlayOnWhite), overlayOnWhite)
        val posterOnWhite = extra.posterOverlay.compositeOver(Color.White)
        assertAa("white on poster overlay", extra.onHero, posterOnWhite)
    }

    @Test
    fun brandFillsAreNeverUsedAsTextColours() {
        // Documented failures from the audit: these must stay below AA so nobody "fixes" the tests by using them.
        assertTrue(contrast(ScpdPalette.Teal500, Color.White) < 4.5)
        assertTrue(contrast(ScpdPalette.Gold500, Color.White) < 4.5)
        assertTrue(scheme.onSecondaryContainer != ScpdPalette.Teal500)
        assertTrue(scheme.onTertiaryContainer != ScpdPalette.Gold500)
    }

    @Test
    fun typeScaleHasNoTextBelowElevenSp() {
        val styles = with(ScpdTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall,
            )
        }
        styles.forEach { assertTrue("${it.fontSize} is below 11sp", it.fontSize.value >= 11f) }
    }

    @Test
    fun touchTargetTokenIsAtLeast48dp() {
        assertTrue(ScpdSpacing().minTouchTarget.value >= 48f)
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}
