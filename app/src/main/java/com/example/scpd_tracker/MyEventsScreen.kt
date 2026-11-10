package com.example.scpd_tracker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.ui.theme.ScpdPalette
import com.example.scpd_tracker.ui.theme.ScpdTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal enum class ArrivalUiStatus { Required, PendingSync, Verified }
internal enum class CheckpointUiStatus { NotOpen, Open, PendingSync, Verified }

internal data class BookingAttendanceUi(
    val arrival: ArrivalUiStatus = ArrivalUiStatus.Required,
    val checkpoint: CheckpointUiStatus = CheckpointUiStatus.NotOpen,
    val requiresCheckpoint: Boolean = true,
    val remainingTime: String = "04:12",
    val checkpointWindow: String = "15:30–15:35",
)

private enum class BookingPage { List, Details, Sending, Failed, Cancelled, OrganiserCancelled }

@Composable
internal fun MyEventsScreen(
    bookedEvents: List<CampusEvent>,
    cancelledEvents: List<CampusEvent>,
    onCancelBooking: (String) -> Boolean,
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onShowNotice: (String) -> Unit,
    attendanceByEvent: Map<String, BookingAttendanceUi> = emptyMap(),
    organiserCancelledEvents: List<CampusEvent> = emptyList(),
    recentlyAttended: List<CampusEvent> = emptyList(),
    wide: Boolean = false,
) {
    var showPast by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(BookingPage.List) }
    var showCancelSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selected = (bookedEvents + cancelledEvents + organiserCancelledEvents)
        .firstOrNull { it.id == selectedId }
    val activeBooking = bookedEvents.firstOrNull { it.id == selectedId }

    BackHandler(showCancelSheet || page != BookingPage.List) {
        when {
            showCancelSheet -> showCancelSheet = false
            page == BookingPage.Sending -> Unit
            else -> {
                page = BookingPage.List
                selectedId = null
            }
        }
    }

    if (showCancelSheet && activeBooking != null) {
        ModalBottomSheet(
            onDismissRequest = { showCancelSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            ReadableWidth {
                Column(
                    Modifier.padding(horizontal = ScpdTheme.spacing.gutter, vertical = ScpdTheme.spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3),
                ) {
                    Text("Cancel your place?", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        activeBooking.title + " · " + activeBooking.date,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Your place goes to another student", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(ScpdTheme.spacing.space3))
                    PrimaryAction("Cancel booking") {
                        val id = activeBooking.id
                        showCancelSheet = false
                        page = BookingPage.Sending
                        scope.launch {
                            delay(650)
                            val cancelled = runCatching { onCancelBooking(id) }.getOrDefault(false)
                            page = if (cancelled) BookingPage.Cancelled else BookingPage.Failed
                        }
                    }
                    SecondaryAction("Keep booking") { showCancelSheet = false }
                    Spacer(Modifier.height(ScpdTheme.spacing.space4))
                }
            }
        }
    }

    when {
        selected != null && page == BookingPage.Details -> BookingDetailsView(
            event = selected,
            attendance = attendanceByEvent[selected.id] ?: BookingAttendanceUi(),
            wide = wide,
            onBack = { page = BookingPage.List },
            onCancel = { showCancelSheet = true },
        )
        selected != null && page == BookingPage.Sending -> CancellationOutcomeView(
            event = selected,
            page = page,
            wide = wide,
            onBack = { page = BookingPage.List },
            onBrowse = onBrowse,
            onRetry = {},
            onShowNotice = onShowNotice,
        )
        selected != null && page == BookingPage.Failed -> CancellationOutcomeView(
            event = selected,
            page = page,
            wide = wide,
            onBack = { page = BookingPage.List },
            onBrowse = onBrowse,
            onRetry = {
                page = BookingPage.Sending
                scope.launch {
                    delay(650)
                    val cancelled = runCatching { onCancelBooking(selected.id) }.getOrDefault(false)
                    page = if (cancelled) BookingPage.Cancelled else BookingPage.Failed
                }
            },
            onShowNotice = onShowNotice,
        )
        selected != null && page == BookingPage.Cancelled -> BookingCancelledView(
            event = selected,
            wide = wide,
            onBack = {
                page = BookingPage.List
                showPast = false
            },
            onBrowse = {
                page = BookingPage.List
                onBrowse()
            },
        )
        selected != null && page == BookingPage.OrganiserCancelled -> CancellationOutcomeView(
            event = selected,
            page = page,
            wide = wide,
            onBack = { page = BookingPage.List },
            onBrowse = onBrowse,
            onRetry = {},
            onShowNotice = onShowNotice,
        )
        else -> MyEventsListView(
            bookedEvents = bookedEvents,
            cancelledEvents = cancelledEvents,
            organiserCancelledEvents = organiserCancelledEvents,
            recentlyAttended = recentlyAttended,
            attendanceByEvent = attendanceByEvent,
            showPast = showPast,
            wide = wide,
            onShowPast = { showPast = it },
            onViewBooking = {
                selectedId = it
                page = BookingPage.Details
            },
            onViewCancelled = { id, byOrganiser ->
                selectedId = id
                page = if (byOrganiser) BookingPage.OrganiserCancelled else BookingPage.Cancelled
            },
            onBrowse = onBrowse,
            onEditInterests = onEditInterests,
            onShowNotice = onShowNotice,
        )
    }
}

@Composable
private fun MyEventsHeader(title: String, wide: Boolean, onBack: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colors.primary)) {
        if (!wide) Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp)
                .padding(horizontal = ScpdTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackButton(onBack, colors.onPrimary)
            Text(
                title,
                Modifier.padding(start = if (onBack == null) 4.dp else 0.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onPrimary,
            )
        }
    }
}

@Composable
private fun MyEventsListView(
    bookedEvents: List<CampusEvent>,
    cancelledEvents: List<CampusEvent>,
    organiserCancelledEvents: List<CampusEvent>,
    recentlyAttended: List<CampusEvent>,
    attendanceByEvent: Map<String, BookingAttendanceUi>,
    showPast: Boolean,
    wide: Boolean,
    onShowPast: (Boolean) -> Unit,
    onViewBooking: (String) -> Unit,
    onViewCancelled: (String, Boolean) -> Unit,
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val urgent = bookedEvents.firstOrNull {
        (attendanceByEvent[it.id] ?: BookingAttendanceUi()).checkpoint == CheckpointUiStatus.Open
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MyEventsHeader("My Events", wide)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.padding(spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    MyEventsTabs(
                        showPast = showPast,
                        empty = bookedEvents.isEmpty(),
                        onShowPast = onShowPast,
                    )
                    if (!showPast) {
                        if (bookedEvents.isEmpty()) {
                            EmptyBookings(onBrowse, onEditInterests)
                        } else {
                            val lead = urgent ?: bookedEvents.first()
                            Text(
                                (if (isEventToday(lead)) "Today · " else "Upcoming · ") +
                                    lead.date.substringBeforeLast(" ").uppercase(Locale.UK),
                                style = MaterialTheme.typography.labelLarge,
                                color = ScpdTheme.colors.textTertiary,
                            )
                            if (urgent != null) {
                                val attendance = attendanceByEvent[urgent.id] ?: BookingAttendanceUi()
                                CheckpointOpenNotice(urgent, attendance, onShowNotice)
                            }
                            bookedEvents.forEachIndexed { index, event ->
                                BookingListCard(
                                    event = event,
                                    featured = index == 0 || event.id == urgent?.id,
                                    attendance = attendanceByEvent[event.id] ?: BookingAttendanceUi(),
                                    onViewBooking = { onViewBooking(event.id) },
                                    onShowNotice = onShowNotice,
                                )
                            }
                            if (recentlyAttended.isNotEmpty()) {
                                Text("Recently attended", style = MaterialTheme.typography.headlineSmall)
                                recentlyAttended.forEach { event ->
                                    PastEventCard(event, "Verified attendance") {}
                                }
                            }
                            AttendanceEligibilityCard {
                                onShowPast(true)
                            }
                        }
                    } else {
                        Text("Past bookings", style = MaterialTheme.typography.headlineSmall)
                        val pastEvents = cancelledEvents + organiserCancelledEvents + recentlyAttended
                        if (pastEvents.isEmpty()) {
                            Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                                Column(
                                    Modifier.fillMaxWidth().padding(spacing.space5),
                                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                                ) {
                                    Text("No previous bookings yet", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Your completed and cancelled events will appear here.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            cancelledEvents.forEach { event ->
                                PastEventCard(event, "Cancelled") { onViewCancelled(event.id, false) }
                            }
                            organiserCancelledEvents.forEach { event ->
                                PastEventCard(event, "Event cancelled") { onViewCancelled(event.id, true) }
                            }
                            recentlyAttended.forEach { event ->
                                PastEventCard(event, "Verified attendance") {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyEventsTabs(showPast: Boolean, empty: Boolean, onShowPast: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(CircleShape)
            .background(colors.outlineVariant).padding(4.dp),
    ) {
        listOf(false to "Upcoming", true to "Past").forEach { (past, label) ->
            val selected = showPast == past
            val light = empty && !past
            Surface(
                onClick = { onShowPast(past) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                color = if (selected) {
                    if (light) colors.surfaceContainerLowest else colors.primary
                } else colors.outlineVariant,
                contentColor = if (selected) {
                    if (light) colors.primary else colors.onPrimary
                } else colors.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyBookings(onBrowse: () -> Unit, onEditInterests: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
        Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.fillMaxWidth().padding(spacing.space5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space5),
            ) {
                Image(
                    painterResource(R.drawable.gilbert),
                    contentDescription = null,
                    modifier = Modifier.size(132.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    "Your next chapter\nis wide open.",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "No upcoming bookings. Explore campus events to find your next one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                PrimaryAction("Browse recommended events", onClick = onBrowse)
                Spacer(Modifier.height(spacing.space2))
            }
        }
        Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
            Column(
                Modifier.fillMaxWidth().padding(spacing.space5),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                Text("Picked around you", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Explore other interests whenever you feel like it.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = onEditInterests) {
                    Text("Manage interests", style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Rounded.NorthEast, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun BookingListCard(
    event: CampusEvent,
    featured: Boolean,
    attendance: BookingAttendanceUi,
    onViewBooking: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
        Column {
            if (featured) MyEventsPoster(event)
            Column(
                Modifier.fillMaxWidth().padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                TagChip("●  Confirmed", colors.secondaryContainer, colors.primary)
                Text(event.title, style = MaterialTheme.typography.headlineMedium)
                Text(event.club, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    EventInfoLine("Date", event.date + " · " + event.time)
                    EventInfoLine("Venue", event.venue)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    EventTags(event.tags)
                    Text("·  1 place booked", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(color = colors.outlineVariant)
                if (featured) {
                    if (attendance.checkpoint != CheckpointUiStatus.Open) {
                        AttendanceRequirementCard(event, attendance)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = ScpdTheme.colors.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(spacing.space2))
                        Text(
                            "Check-in opens on the event day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ScpdTheme.colors.textTertiary,
                        )
                    }
                }
                TextButton(onClick = onViewBooking, contentPadding = PaddingValues(0.dp)) {
                    Text("View booking", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(spacing.space2))
                    Icon(Icons.Rounded.NorthEast, contentDescription = null, Modifier.size(18.dp))
                }
                if (featured) {
                    SecondaryAction("Contact organiser") {
                        onShowNotice("Organiser contact is not available yet.")
                    }
                }
            }
        }
    }
}

@Composable
private fun MyEventsPoster(event: CampusEvent) {
    if (event.id != "build-with-ai") {
        EventPoster(event, featured = true)
        return
    }
    BoxWithConstraints(
        Modifier.fillMaxWidth().clipToBounds().background(ScpdPalette.Navy900),
    ) {
        val width = maxWidth
        Box(Modifier.fillMaxWidth().height((width * 0.46f).coerceAtMost(220.dp))) {
            Image(
                painterResource(R.drawable.orbit_featured_teal),
                contentDescription = null,
                modifier = Modifier.offset(x = width * 0.53f, y = 23.dp).size(width * 0.5f),
            )
            Image(
                painterResource(R.drawable.orbit_featured_yellow),
                contentDescription = null,
                modifier = Modifier.offset(x = width * 0.66f, y = 78.dp).size(width * 0.40f),
            )
            Box(
                Modifier.offset(x = width * 0.56f, y = 4.dp)
                    .rotate(-25f).size(70.dp, 6.dp)
                    .background(ScpdPalette.Gold500),
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(color = ScpdPalette.Teal500, shape = CircleShape) {
                    Text(
                        "TECH / CAMPUS SESSIONS",
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ScpdPalette.Navy900,
                    )
                }
                Text(
                    "BUILD\nWITH AI",
                    style = ScpdTheme.poster.headline,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = ScpdTheme.poster.headline.lineHeight,
                )
                Text(
                    event.date.uppercase(Locale.UK) + "   ↗",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.84f),
                )
            }
        }
    }
}

@Composable
private fun EventInfoLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label + "  ",
            style = MaterialTheme.typography.bodyMedium,
            color = ScpdTheme.colors.textTertiary,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AttendanceRequirementCard(event: CampusEvent, attendance: BookingAttendanceUi) {
    val colors = MaterialTheme.colorScheme
    val spacing = ScpdTheme.spacing
    Surface(color = colors.tertiaryContainer, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            val heading = when (attendance.arrival) {
                ArrivalUiStatus.Required ->
                    if (attendance.requiresCheckpoint) "Two required scans" else "Arrival scan required"
                ArrivalUiStatus.PendingSync -> "Arrival · Pending sync"
                ArrivalUiStatus.Verified -> "Arrival · Verified"
            }
            Text(heading, style = MaterialTheme.typography.titleLarge, color = colors.onTertiaryContainer)
            if (attendance.arrival == ArrivalUiStatus.Required) {
                Text("Scan event QR on arrival.", style = MaterialTheme.typography.bodyLarge)
            }
            if (attendance.requiresCheckpoint) {
                Text(
                    if (attendance.checkpoint == CheckpointUiStatus.Verified) {
                        "Checkpoint · Verified"
                    } else {
                        "Checkpoint · Not open yet"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("One QR checkpoint in the final third", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "QR scans save offline. Manual codes need internet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            syncDeadline(event)?.let {
                Text("Offline sync deadline · " + it, style = MaterialTheme.typography.bodyMedium)
            }
            if (attendance.arrival != ArrivalUiStatus.Required && attendance.requiresCheckpoint) {
                Text(
                    "The organiser announces the five-minute checkpoint in the room.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CheckpointOpenNotice(
    event: CampusEvent,
    attendance: BookingAttendanceUi,
    onShowNotice: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = ScpdTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        Surface(color = colors.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Column(
                Modifier.fillMaxWidth().padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                Text(event.title + " · Checkpoint open", style = MaterialTheme.typography.titleLarge)
                Text(
                    attendance.remainingTime + " left to scan",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ScpdPalette.Teal700,
                )
                Text(
                    if (attendance.arrival == ArrivalUiStatus.PendingSync) {
                        "Arrival pending sync · Checkpoint required"
                    } else "Arrival verified · Checkpoint required",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    attendance.checkpointWindow + " · Five-minute window\nAnnounced in the room",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (attendance.arrival == ArrivalUiStatus.PendingSync) {
                    Text(
                        "Scan the checkpoint even while arrival sync is pending.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    "QR scans work offline. Manual codes need internet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                syncDeadline(event)?.let {
                    Text("Upload saved scans by " + it + ".", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        PrimaryAction("Scan checkpoint QR") {
            onShowNotice("The QR scanner is not connected yet.")
        }
    }
}

private fun isEventToday(event: CampusEvent): Boolean {
    val parser = SimpleDateFormat("EEE d MMM yyyy", Locale.UK)
    val date = runCatching { parser.parse(event.date) }.getOrNull() ?: return false
    val today = Calendar.getInstance()
    val scheduled = Calendar.getInstance().apply { time = date }
    return today.get(Calendar.YEAR) == scheduled.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == scheduled.get(Calendar.DAY_OF_YEAR)
}

private fun syncDeadline(event: CampusEvent): String? {
    val date = runCatching {
        SimpleDateFormat("EEE d MMM yyyy", Locale.UK).parse(event.date)
    }.getOrNull() ?: return null
    val end = event.time.substringAfterLast('–').trim()
    if (!Regex("\\d{2}:\\d{2}").matches(end)) return null
    val nextDay = Calendar.getInstance().apply {
        time = date
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("EEE d MMM", Locale.UK).format(nextDay.time) + ", " + end
}

@Composable
private fun AttendanceEligibilityCard(onView: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(ScpdTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3),
        ) {
            Text("Attendance eligibility", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(color = colors.outlineVariant)
            Text(
                "Both verified scans earn 3 points.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onView, contentPadding = PaddingValues(0.dp)) {
                Text("View attendance history", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Rounded.NorthEast, contentDescription = null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PastEventCard(event: CampusEvent, status: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ScpdTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3),
        ) {
            TagChip(status, colors.surfaceVariant, colors.onSurface)
            Text(event.title, style = MaterialTheme.typography.titleLarge)
            Text(event.club, style = MaterialTheme.typography.titleMedium)
            EventInfoLine("Date", event.date + " · " + event.time)
            EventInfoLine("Venue", event.venue)
        }
    }
}

@Composable
private fun BookingDetailsView(
    event: CampusEvent,
    attendance: BookingAttendanceUi,
    wide: Boolean,
    onBack: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = ScpdTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MyEventsHeader("Your booking", wide, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.padding(spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    Text("Your place is booked", style = MaterialTheme.typography.headlineMedium)
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
                        Column {
                            MyEventsPoster(event)
                            Column(
                                Modifier.padding(spacing.space4),
                                verticalArrangement = Arrangement.spacedBy(spacing.space3),
                            ) {
                                TagChip("✓  Confirmed", colors.secondaryContainer, colors.primary)
                                Text(event.title, style = MaterialTheme.typography.headlineMedium)
                                Text(event.club, style = MaterialTheme.typography.titleMedium)
                                Text(event.date + " · " + event.time, style = MaterialTheme.typography.bodyLarge)
                                Text(event.venue, style = MaterialTheme.typography.bodyLarge)
                                Text("Booking for 1", style = MaterialTheme.typography.bodyMedium)
                                EventTags(event.tags)
                            }
                        }
                    }
                    AttendanceRequirementCard(event, attendance)
                    SecondaryAction("Cancel booking", onClick = onCancel)
                    Spacer(Modifier.height(spacing.space3))
                }
            }
        }
    }
}

@Composable
private fun CancellationOutcomeView(
    event: CampusEvent,
    page: BookingPage,
    wide: Boolean,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onRetry: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = ScpdTheme.spacing
    val sending = page == BookingPage.Sending
    val organiser = page == BookingPage.OrganiserCancelled
    val title = when (page) {
        BookingPage.Sending -> "Cancellation in progress"
        BookingPage.Failed -> "Cancel failed"
        else -> "Event cancelled"
    }
    val lead = when (page) {
        BookingPage.Sending -> "Booking remains active until the server confirms cancellation."
        BookingPage.Failed -> "Booking is still active. Retry cancel or keep it."
        else -> "This booking was cancelled by the organiser."
    }
    val detail = when (page) {
        BookingPage.Sending -> "Confirming cancellation. Wait before trying again."
        BookingPage.Failed -> "We couldn't cancel this booking. Your place is still reserved."
        else -> "The organiser cancelled the event and your booking. No points or streak credit is awarded."
    }
    Column(Modifier.fillMaxSize().background(ScpdPalette.Teal50)) {
        Column(Modifier.background(colors.primary)) {
            MyEventsHeader(title, wide, onBack)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.space5),
                horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    if (sending) CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = colors.onPrimary,
                    )
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onPrimary)
                    Text(
                        lead,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onPrimary.copy(alpha = 0.78f),
                    )
                }
                Image(
                    painterResource(R.drawable.gilbert),
                    contentDescription = null,
                    modifier = Modifier.size(106.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.padding(spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    Text(detail, style = MaterialTheme.typography.bodyLarge)
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
                        Column(
                            Modifier.fillMaxWidth().padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space3),
                        ) {
                            TagChip(
                                when (page) {
                                    BookingPage.Sending -> "Cancellation in progress"
                                    BookingPage.Failed -> "Cancel failed"
                                    else -> "Event cancelled by organiser"
                                },
                                ScpdPalette.Red50,
                                colors.primary,
                            )
                            Text(event.title, style = MaterialTheme.typography.headlineMedium)
                            Text(event.club, style = MaterialTheme.typography.titleMedium)
                            Text(event.date + " · " + event.time, style = MaterialTheme.typography.bodyLarge)
                            Text(event.venue, style = MaterialTheme.typography.bodyLarge)
                            Text("Booking for 1", style = MaterialTheme.typography.bodyLarge)
                            Text(event.tags.joinToString("  /  "), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Text(
                        event.title + " · " + event.date + " · " + event.time + " · " + event.venue,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        BottomActionBar {
            when {
                sending -> SecondaryAction("Back to My Events", onClick = onBack)
                organiser -> {
                    PrimaryAction("Contact organiser") {
                        onShowNotice("Organiser contact is not available yet.")
                    }
                    SecondaryAction("Browse alternatives", onClick = onBrowse)
                }
                else -> {
                    PrimaryAction("Retry cancel", onClick = onRetry)
                    SecondaryAction("Keep booking", onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun BookingCancelledView(
    event: CampusEvent,
    wide: Boolean,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MyEventsHeader("Booking cancelled", wide, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.padding(spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
                        Column(
                            Modifier.fillMaxWidth().padding(spacing.space5),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.space4),
                        ) {
                            Image(
                                painterResource(R.drawable.gilbert),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit,
                            )
                            TagChip("✓  Booking cancelled", colors.secondaryContainer, colors.primary)
                            Text(
                                "Your place has been\nreleased",
                                style = MaterialTheme.typography.headlineLarge,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Booking cancelled. Your place is now available to another student.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                        Column(
                            Modifier.fillMaxWidth().padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space3),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    event.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TagChip("Cancelled", colors.surfaceVariant, colors.onSurface)
                            }
                            Text(event.club, style = MaterialTheme.typography.titleMedium)
                            Text(event.date + " · " + event.time, style = MaterialTheme.typography.bodyLarge)
                            Text(event.venue, style = MaterialTheme.typography.bodyLarge)
                            HorizontalDivider(color = colors.outlineVariant)
                            Text(
                                "Cancelled bookings earn no points or streak credit.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    PrimaryAction("Back to My Events", onClick = onBack)
                    SecondaryAction("Browse other events", onClick = onBrowse)
                }
            }
        }
    }
}
