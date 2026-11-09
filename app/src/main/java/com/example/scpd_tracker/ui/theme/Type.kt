package com.example.scpd_tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.scpd_tracker.R

@OptIn(ExperimentalTextApi::class)
private val Inter = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.inter, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

private fun inter(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Type scale from the Figma spec (frame 5:466), mapped to Material 3 slots.
 * The Figma text style for each slot is listed in sCPD-Figma-Design-Audit.md, section 2.4.
 */
val ScpdTypography = Typography(
    displayLarge = inter(44, 48, FontWeight.Black),
    displayMedium = inter(36, 40, FontWeight.ExtraBold),
    displaySmall = inter(34, 38, FontWeight.Bold), // entry hero
    headlineLarge = inter(32, 36, FontWeight.ExtraBold), // display
    headlineMedium = inter(26, 30, FontWeight.ExtraBold), // title/page
    headlineSmall = inter(24, 28, FontWeight.Bold),
    titleLarge = inter(20, 24, FontWeight.Bold), // title/section
    titleMedium = inter(16, 22, FontWeight.Bold), // title/card
    titleSmall = inter(14, 20, FontWeight.SemiBold),
    bodyLarge = inter(15, 22), // body/large
    bodyMedium = inter(13, 19), // body/medium
    bodySmall = inter(12, 17),
    labelLarge = inter(15, 20, FontWeight.SemiBold), // label/button
    labelMedium = inter(12, 16, FontWeight.SemiBold), // label/chip
    labelSmall = inter(11, 14, FontWeight.SemiBold), // label/overline
)

/** Event poster artwork styles, which sit outside the reading scale. */
@Immutable
data class ScpdPosterType(
    val headline: TextStyle = inter(40, 40, FontWeight.Black),
    val headlineCompact: TextStyle = inter(32, 30, FontWeight.Black),
    /** For long titles or long words, so words wrap whole instead of breaking mid-word. */
    val headlineLong: TextStyle = inter(22, 23, FontWeight.Black),
    val badge: TextStyle = inter(11, 14, FontWeight.Bold),
    val date: TextStyle = inter(12, 16, FontWeight.SemiBold),
)

val LocalPosterType = staticCompositionLocalOf { ScpdPosterType() }
