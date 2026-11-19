package com.example.scpd_tracker.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.R
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.PosterArt
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.theme.PillShape
import com.example.scpd_tracker.ui.theme.ScpdTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Formatting ---------------------------------------------------------------------------------

private val DateLabel = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
private val ShortDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
private val Time = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

val Event.dateLabel: String get() = start.withZoneSameInstant(Policy.ZONE).format(DateLabel)
val Event.timeLabel: String
    get() = start.withZoneSameInstant(Policy.ZONE).format(Time) + "–" + end.withZoneSameInstant(Policy.ZONE).format(Time)
val Event.whenLabel: String get() = "$dateLabel · $timeLabel"

fun LocalDate.label(): String = format(DateLabel)
fun ZonedDateTime.shortLabel(): String = withZoneSameInstant(Policy.ZONE).format(ShortDate) + ", " + format(Time)

fun Event.availability(booked: Boolean): String =
    if (isFull) "Full · $capacity places" else "$placesLeft of $capacity places left" + if (booked) " · You're booked" else ""

// ---- Tags ---------------------------------------------------------------------------------------

fun tagIcon(tag: String): ImageVector = when (tag) {
    "Tech" -> Icons.Rounded.Code
    "Career" -> Icons.Rounded.Work
    "Volunteering" -> Icons.Rounded.VolunteerActivism
    "Leadership" -> Icons.Rounded.Flag
    "Sports" -> Icons.Rounded.EmojiEvents
    "Arts" -> Icons.Rounded.Palette
    else -> Icons.Rounded.Favorite
}

@Composable
internal fun EventTags(tags: List<String>) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2)) {
        tags.forEachIndexed { index, tag ->
            if (index % 2 == 0) TagChip(tag, colors.secondaryContainer, colors.onSecondaryContainer, tagIcon(tag))
            else TagChip(tag, colors.tertiaryContainer, colors.onTertiaryContainer, tagIcon(tag))
        }
    }
}

// ---- Status -------------------------------------------------------------------------------------

/** A stage status as icon + word on a solid container, so it never relies on colour alone. */
@Composable
internal fun StatusChip(status: StageStatus, label: String = status.label) {
    val colors = MaterialTheme.colorScheme
    val (container, content, icon) = when (status) {
        StageStatus.NOT_SCANNED -> Triple(colors.primaryContainer, colors.onSurfaceVariant, Icons.Rounded.RadioButtonUnchecked)
        StageStatus.PENDING_SYNC -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.CloudUpload)
        StageStatus.VERIFIED -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.CheckCircle)
        StageStatus.REJECTED -> Triple(colors.errorContainer, colors.onErrorContainer, Icons.Rounded.Block)
    }
    TagChip(label, container, content, icon)
}

/** Shown at the top of any screen that needs the network. */
@Composable
internal fun OfflineBanner(modifier: Modifier = Modifier, body: String = "You're offline. Saved QR scans upload when you reconnect.") {
    val spacing = ScpdTheme.spacing
    Surface(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, Modifier.size(18.dp))
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---- QR -----------------------------------------------------------------------------------------

/** Draws [payload] as a QR code with ZXing. Black on white, the contrast scanners read best. */
@Composable
internal fun QrImage(payload: String, modifier: Modifier = Modifier, description: String = "Check-in QR code") {
    val bitmap = remember(payload) {
        val size = 512
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) for (y in 0 until size) {
                setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }.asImageBitmap()
    }
    Image(bitmap, description, modifier)
}

// ---- Poster -------------------------------------------------------------------------------------

/** Tallest a poster grows, so on landscape phones and tablets it never fills the whole viewport. */
private val MaxPosterHeight = 240.dp

/**
 * Poster artwork. All of its text comes from the event (category, title, date), so a new event never
 * shows another event's poster copy. Height follows the width at the Figma ratio, capped at
 * [MaxPosterHeight] and at 35% of the screen height.
 */
@Composable
internal fun EventPoster(event: Event, featured: Boolean) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val extra = ScpdTheme.colors
    val poster = ScpdTheme.poster
    val art = event.poster
    val designWidth = if (featured) 372f else 348f
    val designHeight = if (art is PosterArt.Photo) 200f else 160f
    val (background, textColour, badgeColour, badgeText) = when (art) {
        is PosterArt.Photo -> listOf(colors.primary, extra.onHero, colors.secondary, colors.onSecondary)
        PosterArt.Teal -> listOf(colors.secondary, colors.onSecondary, colors.primary, colors.onPrimary)
        PosterArt.Gold -> listOf(colors.tertiary, colors.onTertiary, colors.primary, colors.onPrimary)
    }
    // The poster repeats the title and date shown in real text below it, so it is artwork:
    // hidden from screen readers and drawn at 1x font scale so large text settings cannot overflow it.
    val density = LocalDensity.current
    val maxHeight = minOf(MaxPosterHeight, (LocalConfiguration.current.screenHeightDp * 0.35f).dp)
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            val posterHeight = (maxWidth * (designHeight / designWidth)).coerceAtMost(maxHeight)
            val scale = posterHeight / designHeight.dp
            val posterWidth = maxWidth
            fun fromRight(x: Dp) = posterWidth - (designWidth.dp - x) * scale
            val ringStart = fromRight(if (art is PosterArt.Photo) 210.dp else 205.dp)
            val headlineMaxWidth = (ringStart - spacing.gutter - spacing.space2).coerceAtLeast(120.dp)
            Box(Modifier.fillMaxWidth().height(posterHeight).clipToBounds().background(background)) {
                if (art is PosterArt.Photo) {
                    Image(painterResource(art.image), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.matchParentSize().background(extra.posterOverlay))
                    Image(
                        painterResource(R.drawable.orbit_featured_teal), null,
                        Modifier.offset(fromRight(210.dp), 30.dp * scale).size(160.dp * scale),
                    )
                } else {
                    Image(
                        painterResource(R.drawable.orbit_python_navy), null,
                        Modifier.offset(fromRight(205.dp), 40.dp * scale).size(170.dp * scale),
                    )
                }
                Column(
                    Modifier.fillMaxSize().padding(
                        horizontal = spacing.gutter,
                        vertical = if (art is PosterArt.Photo) spacing.space5 else spacing.space3,
                    ),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(color = badgeColour, contentColor = badgeText, shape = PillShape) {
                        Text(
                            event.tags.joinToString(" / ").uppercase(Locale.UK),
                            Modifier.padding(horizontal = spacing.space3, vertical = spacing.space1),
                            style = poster.badge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val longestWord = event.title.split(' ').maxOf { it.length }
                    Text(
                        event.title.uppercase(Locale.UK),
                        Modifier.widthIn(max = headlineMaxWidth),
                        style = when {
                            event.title.length > 24 || longestWord > 9 -> poster.headlineLong
                            art is PosterArt.Photo -> poster.headline
                            else -> poster.headlineCompact
                        },
                        color = textColour, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Text(event.dateLabel.uppercase(Locale.UK), style = poster.date, color = textColour)
                }
            }
        }
    }
}

/** A labelled screen-reader summary for a block that is drawn as several small texts. */
fun Modifier.summary(text: String): Modifier = clearAndSetSemantics { contentDescription = text }
