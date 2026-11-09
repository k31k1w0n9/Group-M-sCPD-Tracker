package com.example.scpd_tracker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.ui.theme.ScpdTheme

/** UI states expected from the future attendance repository. They are NOT server outcomes yet. */
internal enum class ArrivalUiStatus { Required, PendingSync, Verified }
internal enum class CheckpointUiStatus { NotOpen, Open, PendingSync, Verified }

internal data class BookingAttendanceUi(
    val arrival: ArrivalUiStatus = ArrivalUiStatus.Required,
    val checkpoint: CheckpointUiStatus = CheckpointUiStatus.NotOpen,
    val requiresCheckpoint: Boolean = true,
    val remainingTime: String = "04:12",
)

/**
 * Student booking UI matching docs/5-1-student-wireframes.md.
 *
 * The parent owns the prototype's booking IDs; no Firebase calls or fake server responses
 * are performed here. When the backend is ready, pass real bookings and attendance states.
 */
@Composable
internal fun MyEventsScreen(
    bookedEvents: List<CampusEvent>,
    cancelledEvents: List<CampusEvent>,
    onCancelBooking: (String) -> Unit,
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onShowNotice: (String) -> Unit,
    attendanceByEvent: Map<String, BookingAttendanceUi> = emptyMap(),
    wide: Boolean = false,
) {
    var showPast by rememberSaveable { mutableStateOf(false) }
    var selectedBookingId by rememberSaveable { mutableStateOf<String?>(null) }
    var successId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCancelSheet by rememberSaveable { mutableStateOf(false) }

    val currentBooking = bookedEvents.firstOrNull { it.id == selectedBookingId }
    val cancelledBooking = (bookedEvents + cancelledEvents).firstOrNull { it.id == successId }

    BackHandler(showCancelSheet || selectedBookingId != null || successId != null) {
        when {
            showCancelSheet -> showCancelSheet = false
            successId != null -> successId = null
            else -> selectedBookingId = null
        }
    }

    if (showCancelSheet && currentBooking != null) {
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
                    Text("Cancel your place?", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        currentBooking.title + " · " + currentBooking.date,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Your place goes to another student", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(ScpdTheme.spacing.space2))
                    PrimaryAction("Cancel booking") {
                        val id = currentBooking.id
                        showCancelSheet = false
                        selectedBookingId = null
                        successId = id
                        onCancelBooking(id)
                    }
                    SecondaryAction("Keep booking") { showCancelSheet = false }
                    Spacer(Modifier.height(ScpdTheme.spacing.space4))
                }
            }
        }
    }

    when {
        successId != null && cancelledBooking != null -> BookingCancelledView(
            event = cancelledBooking,
            wide = wide,
            onBack = { successId = null },
            onBrowse = {
                successId = null
                onBrowse()
            },
        )
        currentBooking != null -> BookingDetailsView(
            event = currentBooking,
            attendance = attendanceByEvent[currentBooking.id] ?: BookingAttendanceUi(),
            wide = wide,
            onBack = { selectedBookingId = null },
            onRequestCancel = { showCancelSheet = true },
        )
        else -> MyEventsListView(
            bookedEvents = bookedEvents,
            cancelledEvents = cancelledEvents,
            attendanceByEvent = attendanceByEvent,
            showPast = showPast,
            wide = wide,
            onShowPast = { showPast = it },
            onViewBooking = { selectedBookingId = it },
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
            Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .padding(horizontal = ScpdTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackButton(onBack, colors.onPrimary)
            Text(
                title,
                modifier = Modifier.padding(start = if (onBack == null) 4.dp else 0.dp),
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
    attendanceByEvent: Map<String, BookingAttendanceUi>,
    showPast: Boolean,
    wide: Boolean,
    onShowPast: (Boolean) -> Unit,
    onViewBooking: (String) -> Unit,
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MyEventsHeader("My Events", wide)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.padding(spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape)
                            .background(colors.outlineVariant)
                            .padding(4.dp),
                    ) {
                        listOf(false to "Upcoming", true to "Past").forEach { (past, title) ->
                            Surface(
                                onClick = { onShowPast(past) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                color = if (showPast == past) colors.primary else colors.outlineVariant,
                                contentColor = if (showPast == past) colors.onPrimary else colors.onSurfaceVariant,
                                shape = CircleShape,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }

                    val active = if (showPast) emptyList() else bookedEvents
                    if (active.isEmpty() && !showPast) {
                        EmptyBookings(onBrowse, onEditInterests)
                    } else if (!showPast) {
                        Text(
                            "Upcoming bookings",
                            style = MaterialTheme.typography.labelLarge,
                            color = ScpdTheme.colors.textTertiary,
                        )
                        active.forEach { event ->
                            BookingListCard(
                                event = event,
                                attendance = attendanceByEvent[event.id] ?: BookingAttendanceUi(),
                                onViewBooking = { onViewBooking(event.id) },
                                onShowNotice = onShowNotice,
                            )
                        }
                        RecentlyAttendedDemo()
                    } else {
                        Text("Past bookings", style = MaterialTheme.typography.titleLarge)
                        if (cancelledEvents.isEmpty()) {
                            Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                                Column(Modifier.fillMaxWidth().padding(spacing.space5)) {
                                    Text("No previous bookings yet", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Events you've cancelled will appear here. Verified attendance will appear once connected.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            cancelledEvents.forEach { event ->
                                Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                                    Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                                        TagChip("Cancelled", colors.surfaceVariant, colors.onSurface)
                                        Text(event.title, style = MaterialTheme.typography.titleLarge)
                                        Text(event.club, style = MaterialTheme.typography.titleSmall)
                                        Text(event.date + " · " + event.time, style = MaterialTheme.typography.bodyMedium)
                                        Text("Cancelled bookings earn no points or streak credit.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
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
                    modifier = Modifier.size(130.dp),
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
            }
        }
        Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(spacing.space5), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text("Picked around you", style = MaterialTheme.typography.titleLarge)
                Text("Explore other interests whenever you feel like it.", style = MaterialTheme.typography.bodyLarge)
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
    attendance: BookingAttendanceUi,
    onViewBooking: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column {
            Box(Modifier.clip(MaterialTheme.shapes.large)) { EventPoster(event, featured = true) }
            Column(
                Modifier.padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                TagChip("Confirmed", colors.secondaryContainer, colors.onSecondaryContainer)
                Text(event.title, style = MaterialTheme.typography.headlineMedium)
                Text(event.club, style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text("Date  " + event.date + " · " + event.time, style = MaterialTheme.typography.bodyMedium)
                    Text("Venue  " + event.venue, style = MaterialTheme.typography.bodyMedium)
                }
                EventTags(event.tags)
                Text("1 place booked", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                HorizontalDivider(color = colors.outlineVariant)
                AttendanceRequirementCard(event, attendance, onShowNotice)
                TextButton(onClick = onViewBooking) {
                    Text("View booking", style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Rounded.NorthEast, contentDescription = null)
                }
                SecondaryAction("Contact organiser") {
                    onShowNotice("Organiser contact will be available when the backend is connected.")
                }
            }
        }
    }
}

@Composable
private fun AttendanceRequirementCard(
    event: CampusEvent,
    attendance: BookingAttendanceUi,
    onShowNotice: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val isCheckpointOpen = attendance.requiresCheckpoint && attendance.checkpoint == CheckpointUiStatus.Open
    if (isCheckpointOpen) {
        Surface(color = colors.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(event.title + " · Checkpoint open", style = MaterialTheme.typography.titleMedium)
                Text(attendance.remainingTime + " left to scan", style = MaterialTheme.typography.headlineMedium, color = colors.onSecondaryContainer)
                Text(
                    if (attendance.arrival == ArrivalUiStatus.PendingSync) "Arrival pending sync · Checkpoint required"
                    else "Arrival verified · Checkpoint required",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("Five-minute checkpoint window, announced in the room.", style = MaterialTheme.typography.titleSmall)
                Text("QR scans work offline. Manual codes need internet.", style = MaterialTheme.typography.bodyMedium)
                PrimaryAction("Scan checkpoint QR") {
                    onShowNotice("QR scanner is not connected in this UI prototype.")
                }
            }
        }
    } else {
        Surface(color = colors.tertiaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                val heading = when (attendance.arrival) {
                    ArrivalUiStatus.Required -> if (attendance.requiresCheckpoint) "Two required scans" else "Arrival scan required"
                    ArrivalUiStatus.PendingSync -> "Arrival · Pending sync"
                    ArrivalUiStatus.Verified -> "Arrival · Verified"
                }
                Text(heading, style = MaterialTheme.typography.titleLarge, color = colors.onTertiaryContainer)
                if (attendance.arrival == ArrivalUiStatus.Required) Text("Scan event QR on arrival.", style = MaterialTheme.typography.bodyLarge)
                if (attendance.requiresCheckpoint) {
                    Text(
                        if (attendance.checkpoint == CheckpointUiStatus.Verified) "Checkpoint · Verified" else "Checkpoint · Not open yet",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("One QR checkpoint in the final third", style = MaterialTheme.typography.titleSmall)
                }
                Text("QR scans save offline. Manual codes need internet.", style = MaterialTheme.typography.bodyMedium)
                Text("Offline sync deadline · 24 hours after check-in closes (demo rule)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RecentlyAttendedDemo() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3)) {
        Text("Attendance eligibility", style = MaterialTheme.typography.titleLarge)
        Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(ScpdTheme.spacing.space4), verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2)) {
                Text("Both required scans must be verified to earn 3 points.", style = MaterialTheme.typography.bodyMedium)
                Text("Attendance history will appear here after the verification system is connected.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BookingDetailsView(
    event: CampusEvent,
    attendance: BookingAttendanceUi,
    wide: Boolean,
    onBack: () -> Unit,
    onRequestCancel: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MyEventsHeader("Your booking", wide, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
                    Text("Your place is booked", style = MaterialTheme.typography.headlineMedium)
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                            EventPoster(event, featured = true)
                            Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                                TagChip("Confirmed", colors.secondaryContainer, colors.onSecondaryContainer)
                                Text(event.title, style = MaterialTheme.typography.headlineMedium)
                                Text(event.club, style = MaterialTheme.typography.titleSmall)
                                Text(event.date + " · " + event.time, style = MaterialTheme.typography.bodyLarge)
                                Text(event.venue, style = MaterialTheme.typography.bodyLarge)
                                Text("Booking for 1", style = MaterialTheme.typography.bodyMedium)
                                EventTags(event.tags)
                            }
                        }
                    }
                    AttendanceRequirementCard(event, attendance) {}
                    SecondaryAction("Cancel booking", onClick = onRequestCancel)
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
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
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
                            TagChip("Booking cancelled", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.Check)
                            Text("Your place has been released", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                            Text(
                                "Booking cancelled. Your place is now available to another student.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                            TagChip("Cancelled", colors.surfaceVariant, colors.onSurface)
                            Text(event.title, style = MaterialTheme.typography.titleLarge)
                            Text(event.club, style = MaterialTheme.typography.titleSmall)
                            Text(event.date + " · " + event.time + " · " + event.venue, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                            Text("Cancelled bookings earn no points or streak credit.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    PrimaryAction("Back to My Events", onClick = onBack)
                    SecondaryAction("Browse other events", onClick = onBrowse)
                }
            }
        }
    }
}
