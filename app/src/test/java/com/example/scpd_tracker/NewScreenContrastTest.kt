package com.example.scpd_tracker

import androidx.compose.ui.graphics.Color
import com.example.scpd_tracker.ui.theme.ScpdLightColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Colour pairs the Part 2 screens added: error text (full events, form errors) and the offline banner. */
class NewScreenContrastTest {
    private fun channel(v: Float) = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private val s = ScpdLightColors

    @Test
    fun errorTextPassesAaOnCardsAndBackground() {
        assertTrue(contrast(s.error, s.surfaceContainerLowest) >= 4.5)
        assertTrue(contrast(s.error, s.background) >= 4.5)
    }

    @Test
    fun offlineBannerPassesAa() {
        assertTrue(contrast(s.inverseOnSurface, s.inverseSurface) >= 4.5)
    }

    @Test
    fun statusChipsPassAa() {
        assertTrue(contrast(s.onSurfaceVariant, s.primaryContainer) >= 4.5) // Not scanned
        assertTrue(contrast(s.onTertiaryContainer, s.tertiaryContainer) >= 4.5) // Saved on phone
        assertTrue(contrast(s.onSecondaryContainer, s.secondaryContainer) >= 4.5) // Verified
        assertTrue(contrast(s.onErrorContainer, s.errorContainer) >= 4.5) // Rejected
    }
}
