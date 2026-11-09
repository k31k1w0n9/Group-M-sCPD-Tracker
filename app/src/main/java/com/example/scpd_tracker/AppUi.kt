package com.example.scpd_tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.ui.theme.PillShape
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun BrandIdentities(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 41.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo artwork keeps its exported aspect ratio; only the row around it is flexible.
        Image(painterResource(R.drawable.university_logo), "University of Nottingham", Modifier.size(96.dp, 35.dp))
        Image(painterResource(R.drawable.scpd_logo), "s-CPD", Modifier.size(112.dp, 41.dp))
    }
}

/**
 * Campus photo with the brand overlay. It always fills its parent (audit 1.2) and paints the hero
 * colour first, so white text stays readable if the photo has not drawn yet (audit 3.5).
 */
@Composable
internal fun CampusBackground(modifier: Modifier = Modifier, home: Boolean = false, compact: Boolean = false, committee: Boolean = false) {
    Box(modifier.background(ScpdTheme.colors.heroBackground).clipToBounds()) {
        Image(
            painterResource(if (home) R.drawable.home_campus else if (committee) R.drawable.committee_campus else R.drawable.campus),
            null,
            Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Image(
            painterResource(if (home) R.drawable.home_overlay else if (compact) R.drawable.committee_overlay else R.drawable.role_overlay),
            null,
            Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
    }
}

@Composable
internal fun PrimaryAction(
    label: String,
    enabled: Boolean = true,
    teal: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = spacing.buttonHeight),
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = spacing.space5, vertical = spacing.space3),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (teal) colors.secondary else colors.primary,
            contentColor = if (teal) colors.onSecondary else colors.onPrimary,
            disabledContainerColor = colors.outlineVariant,
            disabledContentColor = colors.onSurfaceVariant,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun SecondaryAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = ScpdTheme.spacing.buttonHeight),
        shape = PillShape,
        border = BorderStroke(1.dp, colors.primary),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surfaceContainerLowest, contentColor = colors.primary),
    ) {
        // Same weight as the primary label so the secondary action never reads as disabled (audit 3.9).
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** 48dp back button with a real, auto-mirrored icon instead of a "←" text glyph (audit 2.7). */
@Composable
internal fun BackButton(onBack: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tint)
    }
}

@Composable
internal fun CompactHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    val spacing = ScpdTheme.spacing
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        BackButton(onBack)
        Column(Modifier.weight(1f)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Status or interest chip on a solid container token (no opacity tints, audit 2.5). */
@Composable
internal fun TagChip(
    label: String,
    container: Color,
    content: Color,
    icon: ImageVector? = null,
) {
    val spacing = ScpdTheme.spacing
    Surface(color = container, contentColor = content, shape = PillShape) {
        Row(
            Modifier.heightIn(min = 28.dp).padding(horizontal = spacing.space3, vertical = spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Placement rule for actions:
 * - A screen has at most one primary action, and it lives here, pinned to the bottom of the viewport.
 * - Secondary actions for the same task stack below the primary one in this bar.
 * - Forms (sign-in, role picker) end with their actions, so the actions still sit at the bottom of the form.
 * - Contextual actions that belong to one piece of content (Clear search in an empty state) stay with
 *   that content and are never filled primary buttons.
 */
@Composable
internal fun BottomActionBar(
    modifier: Modifier = Modifier,
    supporting: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), color = colors.surfaceContainerLowest) {
        Column {
            HorizontalDivider(color = colors.outlineVariant)
            ReadableWidth {
                Column(
                    Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space3),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (supporting != null) {
                        Text(
                            supporting, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                    content()
                }
            }
        }
    }
}

internal enum class BannerTone { Info, Success, Warning }

/**
 * Inline status that stays on the screen it describes. Used instead of pop-up dialogs for errors,
 * confirmations of completed actions and persistent states such as "You're booked".
 */
@Composable
internal fun StatusBanner(title: String, modifier: Modifier = Modifier, body: String? = null, tone: BannerTone = BannerTone.Info) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val (container, iconTint, icon) = when (tone) {
        BannerTone.Info -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.Info)
        BannerTone.Success -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.CheckCircle)
        BannerTone.Warning -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.WarningAmber)
    }
    Surface(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = container, contentColor = colors.onSurface, shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A tappable settings-style row (48dp+). Replaces buttons stacked inside modal sheets. */
@Composable
internal fun ActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    supporting: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val spacing = ScpdTheme.spacing
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = spacing.space4, vertical = spacing.space3),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ScpdTheme.colors.textTertiary)
        }
    }
}

/** Centres content and caps its width on tablets and in landscape (audit 4.8). */
@Composable
internal fun ReadableWidth(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = ScpdTheme.spacing.maxContentWidth).fillMaxWidth(), content = content)
    }
}
