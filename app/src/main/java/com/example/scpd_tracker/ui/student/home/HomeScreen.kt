package com.example.scpd_tracker.ui.student.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.R
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.theme.PillShape
import com.example.scpd_tracker.ui.theme.ScpdTheme

/** Home (ranked for the student) and Explore (every upcoming event). Stateless: it renders [state] only. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onQuery: (String) -> Unit,
    onFilter: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onCheckIn: (String) -> Unit,
    onProgress: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val listState = rememberLazyListState()
    val title = if (state.browsing) "Explore" else "Home"
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.primary).statusBarsPadding()) {
        val keyboardHeight = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
        val short = maxHeight + keyboardHeight < 480.dp
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(colors.background).testTag("home_list")) {
            if (!short) item(key = "campus_header") { HomeHeader(state.browsing, state.greetingName) }
            stickyHeader(key = "event_search") {
                PinnedHomeSearch(if (short) title else null, state.query, onQuery)
            }
            if (state.isOffline) item { OfflineBanner(body = "You're offline. Showing saved events; booking needs a connection.") }
            item {
                ReadableWidth {
                    Column(
                        Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space4),
                        verticalArrangement = Arrangement.spacedBy(spacing.space4),
                    ) {
                        if (!state.browsing) state.happeningNow?.let { HappeningNowCard(it) { onCheckIn(it.id) } }
                        if (!state.browsing) ProgressStrip(state.points, state.streakWeeks, onProgress)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            state.filters.forEach { label -> HomeFilter(label, label == state.filter) { onFilter(label) } }
                        }
                        if (state.showFeatured) {
                            state.featured?.let { featured ->
                                Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                                    SectionHeading("Featured")
                                    FeaturedCard(featured) { onOpenEvent(featured.event.id) }
                                }
                            }
                        }
                        SectionHeading(state.sectionTitle, onSeeAll = if (state.browsing) null else onSeeAll)
                        if (state.query.isNotBlank()) {
                            Text(
                                "${state.cards.size} results for “${state.query}”",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!state.isLoading && state.cards.isEmpty()) NoResults(state.query.isNotBlank() || state.browsing, onClearSearch)
                    }
                }
            }
            items(state.cards, key = { it.event.id }) { card ->
                ReadableWidth {
                    Box(Modifier.padding(start = spacing.gutter, end = spacing.gutter, bottom = spacing.space4)) {
                        EventCard(card) { onOpenEvent(card.event.id) }
                    }
                }
            }
        }
    }
}

/** The one thing a booked student needs on event day, one tap from Home. */
@Composable
private fun HappeningNowCard(event: Event, onCheckIn: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.secondaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("happening_now")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            TagChip("Happening now", colors.surfaceContainerLowest, colors.onSecondaryContainer, Icons.Rounded.QrCodeScanner)
            Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${event.timeLabel} · ${event.venue}", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (event.requiresCheckpoint) "Scan on arrival, then again at the checkpoint." else "Scan the QR on the organiser's screen.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
            PrimaryAction("Check in", onClick = onCheckIn)
        }
    }
}

/** Points and streak at a glance; the whole row opens Progress. */
@Composable
private fun ProgressStrip(points: Int, streakWeeks: Int, onClick: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large,
        modifier = Modifier.summary("$points s-CPD points, $streakWeeks-week streak. Open progress."),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = spacing.space4, vertical = spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = colors.onTertiaryContainer)
            Text("$points points", style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.onSecondaryContainer)
            Text(if (streakWeeks == 1) "1-week streak" else "$streakWeeks-week streak", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ScpdTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun NoResults(searching: Boolean, onClear: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space6), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Text(if (searching) "No events match" else "No events yet", style = MaterialTheme.typography.titleLarge)
            Text(
                if (searching) "Try a shorter search or another interest." else "New events appear here once the s-CPD office approves them.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (searching) SecondaryAction("Clear search", onClick = onClear)
        }
    }
}

/**
 * The campus artwork scrolls with the feed. Search is the next item and stays pinned on scroll.
 */
@Composable
private fun HomeHeader(browsing: Boolean, name: String) {
    val spacing = ScpdTheme.spacing
    val extra = ScpdTheme.colors
    Box(Modifier.fillMaxWidth()) {
        CampusBackground(Modifier.matchParentSize(), home = true)
        ReadableWidth {
            Column(Modifier.padding(start = spacing.gutter, end = spacing.gutter, bottom = spacing.space2)) {
                BrandIdentities(Modifier.padding(top = spacing.space2, bottom = spacing.space2))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (browsing) "Explore" else if (name.isBlank()) "Hello!" else "Hello, $name!",
                            Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = extra.onHero,
                        )
                        Text(
                            if (browsing) "Every approved event, in one place." else "Discover events, clubs and skills.",
                            style = MaterialTheme.typography.bodySmall, color = extra.onHeroMuted,
                        )
                    }
                    GilbertWave()
                }
            }
        }
    }
}

@Composable
private fun GilbertWave(modifier: Modifier = Modifier) {
    // Crops one pose out of the Gilbert sprite sheet; the numbers are sprite coordinates, not layout.
    Box(modifier.size(52.dp).clipToBounds()) {
        Image(
            painterResource(R.drawable.gilbert_sheet), null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = 4f
                scaleY = 4f
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = -4.935.dp.toPx() * (52f / 75f)
                translationY = -5.9175.dp.toPx() * (52f / 75f)
            },
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun HomeSearch(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val spacing = ScpdTheme.spacing
    val extra = ScpdTheme.colors
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value, onValueChange = onChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Search events, clubs or skills" },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = extra.onHero),
        cursorBrush = SolidColor(extra.onHero),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = spacing.minTouchTarget)
                    .background(extra.heroFieldContainer, shape).border(1.dp, extra.heroFieldBorder, shape)
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            "Search events, clubs or skills", style = MaterialTheme.typography.bodyLarge,
                            color = extra.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                Icon(Icons.Rounded.Search, contentDescription = null, Modifier.size(20.dp), tint = extra.onHero)
            }
        },
    )
}

/** Material filter chip: 36dp visual height, 48dp touch target enforced by Material. */
@Composable
private fun HomeFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.heightIn(min = 36.dp),
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        shape = PillShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerLowest, labelColor = colors.onSurface,
            selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary, selectedLeadingIconColor = colors.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = colors.outlineVariant, selectedBorderColor = colors.primary),
    )
}

@Composable
private fun SectionHeading(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = ScpdTheme.spacing.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2),
    ) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text("See all", style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun CardTitle(title: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Rounded.NorthEast, contentDescription = null, Modifier.size(24.dp))
    }
}

@Composable
private fun EventFacts(card: EventCardUi) {
    val type = MaterialTheme.typography
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val event = card.event
    Column(verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space1)) {
        Text(event.clubName, style = type.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${event.whenLabel} · ${event.venue}", style = type.bodyMedium, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            event.availability(card.booked), style = type.bodyMedium,
            color = if (event.isFull && !card.booked) MaterialTheme.colorScheme.error else muted,
        )
    }
}

@Composable
private fun FeaturedCard(card: EventCardUi, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(onClick = onOpen, color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column {
            EventPoster(card.event, featured = true)
            // The poster already carries the title in large type, so the card shows the facts only.
            Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(card.event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                EventFacts(card)
                EventTags(card.event.tags)
            }
        }
    }
}

@Composable
private fun EventCard(card: EventCardUi, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onOpen, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("event_card")) {
        Column(Modifier.padding(spacing.space3), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Box(Modifier.clip(MaterialTheme.shapes.medium)) { EventPoster(card.event, featured = false) }
            Column(Modifier.padding(spacing.space1), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                CardTitle(card.event.title)
                EventFacts(card)
                // One short, specific reason from the ranker instead of a generic "For you" badge.
                Text(card.reason, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
            }
        }
    }
}

/** One search field stays in the same composition as it pins, preserving focus and query. */
@Composable
private fun PinnedHomeSearch(title: String?, query: String, onQuery: (String) -> Unit) {
    val spacing = ScpdTheme.spacing
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)) {
        ReadableWidth {
            Column(Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space2)) {
                if (title != null) {
                    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = ScpdTheme.colors.onHero)
                    Spacer(Modifier.height(spacing.space2))
                }
                HomeSearch(query, onQuery)
            }
        }
    }
}
