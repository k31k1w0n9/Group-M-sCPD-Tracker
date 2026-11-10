package com.example.scpd_tracker

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.ui.theme.PillShape
import com.example.scpd_tracker.ui.theme.ScpdTheme
import kotlinx.coroutines.launch

/** How an event poster is drawn. Every poster's text comes from the event itself, never hard-coded. */
internal sealed interface PosterArt {
    data class Photo(@DrawableRes val image: Int) : PosterArt
    data object Teal : PosterArt
    data object Gold : PosterArt
}

internal data class CampusEvent(
    val id: String,
    val title: String,
    val club: String,
    val date: String,
    val time: String,
    val venue: String,
    val remaining: Int,
    val capacity: Int,
    val tags: List<String>,
    val poster: PosterArt,
    /** A reason based on the student's history, when the recommender has one. */
    val historyReason: String? = null,
) {
    val isFull get() = remaining <= 0

    fun availability(booked: Boolean): String {
        val left = remaining - if (booked) 1 else 0
        return if (left <= 0) "Full · $capacity places" else "$left of $capacity places left"
    }

    fun reasonFor(interests: List<String>): String =
        historyReason
            ?: tags.firstOrNull { it in interests }?.let { "Matches your $it interest" }
            ?: "Something new to try"
}

// shortcut: approved local sample events, replace this feed with the repository when the backend is connected.
// The samples deliberately differ in title length, club, poster style and capacity so layouts are tested
// against realistic data rather than three identical cards.
internal val HomeEvents = listOf(
    CampusEvent(
        id = "build-with-ai", title = "Build with AI", club = "Computer Science Society",
        date = "Wed 18 Nov 2026", time = "14:00–16:00", venue = "F1A02",
        remaining = 12, capacity = 40, tags = listOf("Tech", "Career"),
        poster = PosterArt.Photo(R.drawable.poster_featured_photo),
    ),
    CampusEvent(
        id = "python-next-steps", title = "Python: Next Steps", club = "Computer Science Society",
        date = "Fri 20 Nov 2026", time = "15:00–17:00", venue = "TCR1",
        remaining = 16, capacity = 35, tags = listOf("Tech"),
        poster = PosterArt.Teal, historyReason = "Because you attended Intro to Python",
    ),
    CampusEvent(
        id = "garden-volunteering", title = "Semenyih Community Garden Volunteering Morning",
        club = "Rotaract Club of the University of Nottingham Malaysia",
        date = "Sat 21 Nov 2026", time = "08:30–12:30", venue = "Community garden, bus from Main Gate",
        remaining = 0, capacity = 25, tags = listOf("Volunteering"), poster = PosterArt.Gold,
    ),
)

private enum class HomeTab(val label: String) { Home("Home"), Explore("Explore"), MyEvents("My Events"), Profile("Profile") }

@Composable
internal fun StudentHome(
    interests: List<String>,
    snackbar: SnackbarHostState,
    onEditInterests: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var query by rememberSaveable { mutableStateOf("") }
    var shortcut by rememberSaveable { mutableStateOf("For you") }
    var tab by rememberSaveable { mutableStateOf(HomeTab.Home) }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }
    // shortcut: bookings are kept in memory for the prototype; the booking repository replaces this.
    var bookedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var cancelledIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val browsing = tab == HomeTab.Explore
    val selectedEvent = HomeEvents.firstOrNull { it.id == selectedEventId }
    val compact by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280 }
    }
    val showFeatured = query.isBlank() && shortcut == "For you" && !browsing
    val featured = HomeEvents.first()
    val visibleEvents = HomeEvents.filter { event ->
        // The featured event is not repeated in the list below it (audit 3.9).
        !(showFeatured && event == featured) &&
            (shortcut in listOf("For you", "All interests") || shortcut in event.tags) &&
            (query.isBlank() || listOf(event.title, event.club, event.venue, event.tags.joinToString(" "))
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(selectedEvent != null) { selectedEventId = null }
    BackHandler(selectedEvent == null && (browsing || tab == HomeTab.MyEvents || tab == HomeTab.Profile || query.isNotEmpty()) && !keyboardVisible) {
        query = ""
        tab = HomeTab.Home
        shortcut = "For you"
        focus.clearFocus()
    }
    val title = tab.label
    val destinations = homeDestinations(
        onHome = {
            tab = HomeTab.Home
            selectedEventId = null
            shortcut = "For you"
            query = ""
            scope.launch { listState.animateScrollToItem(0) }
        },
        onExplore = {
            tab = HomeTab.Explore
            selectedEventId = null
            shortcut = "All interests"
            scope.launch { listState.animateScrollToItem(0) }
        },
        onProfile = {
            tab = HomeTab.Profile
            selectedEventId = null
        },
        onMyEvents = {
            tab = HomeTab.MyEvents
            selectedEventId = null
        },
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        // Adaptive rules (audit 4.7 and 4.8). From 600dp wide (phone landscape, tablets) the bottom bar
        // becomes a side rail. When the height is short (landscape phone), the tall photo hero is replaced
        // by the compact header so content is visible without scrolling.
        val wide = maxWidth >= 600.dp
        val short = maxHeight < 480.dp
        if (wide) {
            // Navy strip behind the light status-bar icons, so the clock stays readable above the white rail.
            Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(colors.primary))
        }
        Row(
            Modifier.fillMaxSize().then(
                // windowInsetsPadding also consumes the inset, so the rail and header below don't pad twice.
                if (wide) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier,
            ),
        ) {
            if (wide) HomeRail(title, destinations)
            Column(Modifier.weight(1f).fillMaxHeight().imePadding()) {
                Box(Modifier.weight(1f)) {
                    when {
                        // Event details and Profile are screens in the shell, not modal sheets, so the
                        // navigation stays available and Back works the same way everywhere.
                        selectedEvent != null -> EventDetailsScreen(
                            event = selectedEvent,
                            booked = selectedEvent.id in bookedIds,
                            onBack = { selectedEventId = null },
                            onBook = { bookedIds = bookedIds + selectedEvent.id },
                        )
                        tab == HomeTab.Profile -> ProfileScreen(interests, onEditInterests, onSignOut)
                        tab == HomeTab.MyEvents -> MyEventsScreen(
                            bookedEvents = HomeEvents.filter { it.id in bookedIds },
                            cancelledEvents = HomeEvents.filter { it.id in cancelledIds },
                            onCancelBooking = { id ->
                                val exists = id in bookedIds
                                if (exists) {
                                    bookedIds = bookedIds - id
                                    cancelledIds = (cancelledIds + id).distinct()
                                }
                                exists
                            },
                            onBrowse = {
                                tab = HomeTab.Explore
                                shortcut = "All interests"
                            },
                            onEditInterests = onEditInterests,
                            onShowNotice = { message -> scope.launch { snackbar.showSnackbar(message) } },
                            wide = wide,
                        )
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // With no bottom bar, the list itself keeps clear of the system navigation bar.
                                contentPadding = if (wide) WindowInsets.navigationBars.asPaddingValues() else PaddingValues(),
                            ) {
                                item {
                                    if (short) CompactHomeHeader(title, query) { query = it }
                                    else HomeHeader(browsing, query, onQuery = { query = it })
                                }
                                item {
                                    ReadableWidth {
                                        Column(
                                            Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space5),
                                            verticalArrangement = Arrangement.spacedBy(spacing.space6),
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                                            ) {
                                                (listOf("For you") + interests.take(2) + "All interests").distinct().forEach { label ->
                                                    HomeFilter(label, label == shortcut) { shortcut = label }
                                                }
                                            }
                                            if (showFeatured) {
                                                Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                                                    SectionHeading("Featured")
                                                    FeaturedCard(featured, featured.id in bookedIds) { selectedEventId = featured.id }
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                                                SectionHeading(
                                                    if (browsing || query.isNotBlank()) "Events" else "Recommended for you",
                                                    onSeeAll = if (browsing) null else {
                                                        {
                                                            tab = HomeTab.Explore
                                                            shortcut = "All interests"
                                                        }
                                                    },
                                                )
                                                if (query.isNotBlank()) {
                                                    Text(
                                                        "${visibleEvents.size} results for “$query”",
                                                        style = type.bodyMedium, color = colors.onSurfaceVariant,
                                                    )
                                                }
                                                if (visibleEvents.isEmpty()) {
                                                    NoResults {
                                                        query = ""
                                                        shortcut = "All interests"
                                                    }
                                                }
                                                visibleEvents.forEach { event ->
                                                    EventCard(event, interests, event.id in bookedIds) { selectedEventId = event.id }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (compact) CompactHomeHeader(title, query) { query = it }
                        }
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                }
                if (!wide && !keyboardVisible) HomeNavigation(title, destinations)
            }
        }
    }
}

/** Empty search result. Its action is contextual, so it stays with the message as a secondary button. */
@Composable
private fun NoResults(onClear: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space6), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Text("No events match", style = MaterialTheme.typography.titleLarge)
            Text(
                "Try a shorter search or another interest.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryAction("Clear search", onClick = onClear)
        }
    }
}

/** Navy title bar for screens inside the student shell. */
@Composable
private fun ShellTopBar(title: String, onBack: (() -> Unit)? = null) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = ScpdTheme.colors.onHero) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp)
                .padding(start = if (onBack != null) spacing.space1 else spacing.gutter, end = spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            if (onBack != null) BackButton(onBack, tint = ScpdTheme.colors.onHero)
            Text(
                title, Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Event details as a full screen: one title, the facts, and the booking action pinned at the bottom.
 * Booking status is shown inline at the top instead of in a dialog.
 */
@Composable
private fun EventDetailsScreen(event: CampusEvent, booked: Boolean, onBack: () -> Unit, onBook: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize()) {
        ShellTopBar("Event details", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    if (booked) StatusBanner("You're booked", tone = BannerTone.Success)
                    Box(Modifier.clip(MaterialTheme.shapes.large)) { EventPoster(event, featured = false) }
                    Text(event.title, Modifier.semantics { heading() }, style = type.headlineMedium)
                    EventTags(event.tags)
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                        Text(event.club, style = type.titleSmall)
                        Text("${event.date} · ${event.time}", style = type.bodyLarge)
                        Text(event.venue, style = type.bodyLarge)
                    }
                    Text(
                        "Earn 3 points once your attendance is verified.",
                        style = type.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // A full event says so once, with the next step, instead of a banner plus a label plus a button.
        val supporting = if (event.isFull && !booked) {
            "All ${event.capacity} places are taken. Check again later."
        } else {
            event.availability(booked)
        }
        BottomActionBar(supporting = supporting) {
            when {
                booked -> PrimaryAction("Booked", enabled = false) {}
                event.isFull -> PrimaryAction("Event full", enabled = false) {}
                else -> PrimaryAction("Book event", onClick = onBook)
            }
        }
    }
}

/** Profile as a tab screen. Rows replace the old modal sheet of stacked buttons. */
@Composable
private fun ProfileScreen(interests: List<String>, onEditInterests: () -> Unit, onSignOut: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ShellTopBar("Profile")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                    ) {
                        Box(
                            Modifier.size(56.dp).background(colors.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("JD", style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer)
                        }
                        Column {
                            Text("John Doe", style = MaterialTheme.typography.titleLarge)
                            Text("Student", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(spacing.space2))
                    ActionRow(
                        "Interests", Icons.Rounded.Interests, onEditInterests,
                        supporting = interests.joinToString(", ").ifEmpty { "None chosen" },
                    )
                    ActionRow("Sign out", Icons.AutoMirrored.Rounded.Logout, onSignOut, tint = colors.error)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(browsing: Boolean, query: String, onQuery: (String) -> Unit) {
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    val extra = ScpdTheme.colors
    // Hug-height header: every block sizes to its content, nothing is clipped (audit 1.2 and 1.3).
    Box(Modifier.fillMaxWidth()) {
        CampusBackground(Modifier.matchParentSize(), home = true)
        ReadableWidth(Modifier.statusBarsPadding()) {
            Column(Modifier.padding(horizontal = spacing.gutter)) {
                BrandIdentities(Modifier.padding(vertical = spacing.space6))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    // One heading per screen: the greeting on Home, the tab name on Explore.
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                        Text(
                            if (browsing) "Explore" else "Hello, John!", Modifier.semantics { heading() },
                            style = type.headlineMedium, color = extra.onHero,
                        )
                        Text(
                            if (browsing) "Every approved event, in one place." else "Discover events, clubs and skills.",
                            style = type.bodyMedium, color = extra.onHeroMuted,
                        )
                    }
                    GilbertWave()
                }
                HomeSearch(query, onQuery, Modifier.padding(vertical = spacing.space5))
            }
        }
    }
}

@Composable
private fun GilbertWave(modifier: Modifier = Modifier) {
    // Crops one pose out of the Gilbert sprite sheet; the numbers are sprite coordinates, not layout.
    Box(modifier.size(75.dp).clipToBounds()) {
        Image(
            painterResource(R.drawable.gilbert_sheet), null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = 4f
                scaleY = 4f
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = -4.935.dp.toPx()
                translationY = -5.9175.dp.toPx()
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
                    .background(extra.heroFieldContainer, shape)
                    .border(1.dp, extra.heroFieldBorder, shape)
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

/** Material filter chip: 36dp visual height, 48dp touch target enforced by Material (audit 3.10). */
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
            containerColor = colors.surfaceContainerLowest,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = colors.outlineVariant, selectedBorderColor = colors.primary,
        ),
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
        // Only shown when it leads somewhere new; a TextButton keeps the 48dp touch target.
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun tagIcon(tag: String): ImageVector = when (tag) {
    "Tech" -> Icons.Rounded.Code
    "Career" -> Icons.Rounded.Work
    "Volunteering" -> Icons.Rounded.VolunteerActivism
    else -> Icons.Rounded.Favorite
}

@Composable
internal fun EventTags(tags: List<String>) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2)) {
        tags.forEachIndexed { index, tag ->
            if (index % 2 == 0) {
                TagChip(tag, colors.secondaryContainer, colors.onSecondaryContainer, tagIcon(tag))
            } else {
                TagChip(tag, colors.tertiaryContainer, colors.onTertiaryContainer, tagIcon(tag))
            }
        }
    }
}

/** Card title row: the whole card is the tap target, so the arrow is decoration only (audit 3.9). */
@Composable
private fun CardTitle(title: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2),
    ) {
        Text(
            title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Rounded.NorthEast, contentDescription = null, Modifier.size(24.dp))
    }
}

/** The three lines every event card shows: club, when and where, and availability. */
@Composable
private fun EventFacts(event: CampusEvent, booked: Boolean) {
    val type = MaterialTheme.typography
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space1)) {
        Text(event.club, style = type.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            "${event.date} · ${event.time} · ${event.venue}", style = type.bodyMedium, color = muted,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (booked) "Booked · ${event.availability(true)}" else event.availability(false),
            style = type.bodyMedium, color = if (event.isFull && !booked) MaterialTheme.colorScheme.error else muted,
        )
    }
}

@Composable
private fun FeaturedCard(event: CampusEvent, booked: Boolean, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(onClick = onOpen, color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column {
            EventPoster(event, featured = true)
            Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                CardTitle(event.title)
                EventFacts(event, booked)
                EventTags(event.tags)
            }
        }
    }
}

@Composable
private fun EventCard(event: CampusEvent, interests: List<String>, booked: Boolean, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onOpen, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(spacing.space3), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Box(Modifier.clip(MaterialTheme.shapes.medium)) { EventPoster(event, featured = false) }
            Column(
                Modifier.padding(horizontal = spacing.space1, vertical = spacing.space1),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                CardTitle(event.title)
                EventFacts(event, booked)
                // One short, specific reason instead of a generic "For you" badge plus a sentence.
                Text(
                    event.reasonFor(interests), style = MaterialTheme.typography.labelMedium,
                    color = colors.onSecondaryContainer,
                )
            }
        }
    }
}

/** Tallest a poster grows, so on landscape phones and tablets it never fills the whole viewport. */
private val MaxPosterHeight = 240.dp

/**
 * Poster artwork. All of its text comes from the event (category, title, date), so a new event never
 * shows another event's poster copy. Height follows the width at the Figma ratio, capped at
 * [MaxPosterHeight]. One orbit ring is kept as the brand motif; the extra ring and the floating accent
 * bar were decoration only and are removed.
 */
@Composable
internal fun EventPoster(event: CampusEvent, featured: Boolean) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val extra = ScpdTheme.colors
    val poster = ScpdTheme.poster
    val designWidth = if (featured) 372f else 348f
    val designHeight = if (event.poster is PosterArt.Photo) 200f else 160f
    val (background, textColour, badgeColour, badgeText) = when (event.poster) {
        is PosterArt.Photo -> listOf(colors.primary, extra.onHero, colors.secondary, colors.onSecondary)
        PosterArt.Teal -> listOf(colors.secondary, colors.onSecondary, colors.primary, colors.onPrimary)
        PosterArt.Gold -> listOf(colors.tertiary, colors.onTertiary, colors.primary, colors.onPrimary)
    }
    // The poster repeats the title and date shown in real text just below it, so it is artwork:
    // hidden from screen readers and drawn at 1x font scale so large text settings cannot overflow it.
    val density = LocalDensity.current
    // On short screens (landscape) the poster also stays under 35% of the height, so the title is visible.
    val maxHeight = minOf(MaxPosterHeight, (LocalConfiguration.current.screenHeightDp * 0.35f).dp)
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        val posterHeight = (maxWidth * (designHeight / designWidth)).coerceAtMost(maxHeight)
        // Orbit art uses Figma poster coordinates, scaled with the height and anchored to the right edge.
        val scale = posterHeight / designHeight.dp
        val posterWidth = maxWidth
        fun fromRight(x: Dp) = posterWidth - (designWidth.dp - x) * scale
        val ringStart = fromRight(if (event.poster is PosterArt.Photo) 210.dp else 205.dp)
        // Headline stops before the orbit ring, so text never overlaps the art (navy on navy vanished).
        val headlineMaxWidth = (ringStart - spacing.gutter - spacing.space2).coerceAtLeast(120.dp)
        Box(Modifier.fillMaxWidth().height(posterHeight).clipToBounds().background(background)) {
            if (event.poster is PosterArt.Photo) {
                Image(
                    painterResource(event.poster.image), null,
                    Modifier.matchParentSize(), contentScale = ContentScale.Crop,
                )
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
            // Badge, headline and date are stacked so longer text pushes down instead of overlapping.
            Column(
                Modifier.fillMaxSize().padding(
                    horizontal = spacing.gutter,
                    vertical = if (event.poster is PosterArt.Photo) spacing.space5 else spacing.space3,
                ),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(color = badgeColour, contentColor = badgeText, shape = PillShape) {
                    Text(
                        event.tags.joinToString(" / ").uppercase(),
                        Modifier.padding(horizontal = spacing.space3, vertical = spacing.space1),
                        style = poster.badge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                // Long titles or long words step down a size, so words wrap whole rather than mid-word.
                val longestWord = event.title.split(' ').maxOf { it.length }
                Text(
                    event.title.uppercase(),
                    Modifier.widthIn(max = headlineMaxWidth),
                    style = when {
                        event.title.length > 24 || longestWord > 9 -> poster.headlineLong
                        event.poster is PosterArt.Photo -> poster.headline
                        else -> poster.headlineCompact
                    },
                    color = textColour, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                Text(event.date.uppercase(), style = poster.date, color = textColour)
            }
        }
    }
    }
}

private data class NavDestination(val label: String, val icon: ImageVector, val action: (() -> Unit)?)

// Progress is still planned. My Events now uses the same local booking state as Event details.
private fun homeDestinations(onHome: () -> Unit, onExplore: () -> Unit, onProfile: () -> Unit, onMyEvents: () -> Unit) = listOf(
    NavDestination("Home", Icons.Rounded.Home, onHome),
    NavDestination("Explore", Icons.Rounded.Explore, onExplore),
    NavDestination("My Events", Icons.Rounded.ConfirmationNumber, onMyEvents),
    NavDestination("Progress", Icons.Rounded.Insights, null),
    NavDestination("Profile", Icons.Rounded.AccountCircle, onProfile),
)

@Composable
private fun NavLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Compact widths: Material 3 navigation bar with 48dp+ items, one selected indicator, and unselected
 * labels in text-tertiary (6.10:1) instead of #707D89 (4.21:1). Audit 1.7 and 3.1.
 */
@Composable
private fun HomeNavigation(selected: String, destinations: List<NavDestination>) {
    val colors = MaterialTheme.colorScheme
    val muted = ScpdTheme.colors.textTertiary
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.onSurface,
        selectedTextColor = colors.onSurface,
        indicatorColor = colors.secondaryContainer,
        unselectedIconColor = muted,
        unselectedTextColor = muted,
        disabledIconColor = muted,
        disabledTextColor = muted,
    )
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        NavigationBar(containerColor = colors.surfaceContainerLowest, tonalElevation = 0.dp) {
            destinations.forEach { destination ->
                NavigationBarItem(
                    selected = destination.label == selected,
                    onClick = { destination.action?.invoke() },
                    enabled = destination.action != null,
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { NavLabel(destination.label) },
                    colors = itemColors,
                )
            }
        }
    }
}

/** Medium and expanded widths (600dp+, including phone landscape): the same destinations as a side rail. */
@Composable
private fun HomeRail(selected: String, destinations: List<NavDestination>) {
    val colors = MaterialTheme.colorScheme
    val muted = ScpdTheme.colors.textTertiary
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onSurface,
        selectedTextColor = colors.onSurface,
        indicatorColor = colors.secondaryContainer,
        unselectedIconColor = muted,
        unselectedTextColor = muted,
        disabledIconColor = muted,
        disabledTextColor = muted,
    )
    Row {
        NavigationRail(
            containerColor = colors.surfaceContainerLowest,
            // The rail also keeps clear of a camera cutout on the left in landscape.
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        ) {
            Spacer(Modifier.weight(1f))
            destinations.forEach { destination ->
                NavigationRailItem(
                    selected = destination.label == selected,
                    onClick = { destination.action?.invoke() },
                    enabled = destination.action != null,
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { NavLabel(destination.label) },
                    colors = itemColors,
                )
            }
            Spacer(Modifier.weight(1f))
        }
        VerticalDivider(color = colors.outlineVariant)
    }
}

/** Navy title bar with search. Sticky on scroll, and the whole header when the screen is short. */
@Composable
private fun CompactHomeHeader(title: String, query: String, onQuery: (String) -> Unit) {
    val spacing = ScpdTheme.spacing
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)) {
        ReadableWidth(Modifier.statusBarsPadding()) {
            Column(Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space3)) {
                Text(
                    title, Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge, color = ScpdTheme.colors.onHero,
                )
                Spacer(Modifier.height(spacing.space3))
                HomeSearch(query, onQuery)
            }
        }
    }
}
