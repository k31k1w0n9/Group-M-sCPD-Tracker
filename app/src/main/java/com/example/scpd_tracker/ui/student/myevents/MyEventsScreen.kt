package com.example.scpd_tracker.ui.student.myevents

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.R
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.student.details.AttendanceRequirement
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun MyEventsRoute(
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onCheckIn: (String) -> Unit,
    onHistory: () -> Unit,
    initialEventId: String? = null,
) {
    val vm = scpdViewModel { c, s -> MyEventsViewModel(c.events, c.bookings, c.attendance, c.network, c.clock, s) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Saved across rotation, so the booking from a notification opens once and not again on every rotation.
    var handled by rememberSaveable(initialEventId) { mutableStateOf(false) }
    LaunchedEffect(initialEventId, state.upcoming, state.waitlisted, state.past) {
        if (handled || initialEventId == null) return@LaunchedEffect
        val match = (state.upcoming + state.waitlisted).firstOrNull { it.event.id == initialEventId }
        when {
            match != null -> {
                vm.openBooking(match.booking.id)
                handled = true
            }
            state.past.any { it.event.id == initialEventId } -> {
                vm.selectTab(MyEventsTab.Past)
                handled = true
            }
        }
    }
    ShowMessages(vm.messages.messages)
    BackHandler(state.page != MyEventsPage.List) { vm.backToList() }
    when (state.page) {
        MyEventsPage.List -> MyEventsList(state, vm::selectTab, vm::openBooking, onBrowse, onEditInterests, onCheckIn, onHistory)
        MyEventsPage.Details -> BookingDetails(state, vm::backToList, vm::askCancel, vm::dismissCancel, vm::confirmCancel, onCheckIn)
        MyEventsPage.Cancelled -> BookingCancelled(state, vm::backToList, onBrowse)
    }
}

@Composable
internal fun MyEventsList(
    state: MyEventsUiState,
    onTab: (MyEventsTab) -> Unit,
    onOpen: (String) -> Unit,
    onBrowse: () -> Unit,
    onEditInterests: () -> Unit,
    onCheckIn: (String) -> Unit,
    onHistory: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShellTopBar("My Events")
        if (state.isOffline) OfflineBanner()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    MyEventsTabs(state.tab, state.waitlisted.size, onTab)
                    when (state.tab) {
                        MyEventsTab.Upcoming ->
                            if (state.upcoming.isEmpty()) EmptyBookings(onBrowse, onEditInterests)
                            else state.upcoming.forEach { BookingCard(it, onOpen = { onOpen(it.booking.id) }, onCheckIn = { onCheckIn(it.event.id) }) }
                        MyEventsTab.Waitlist ->
                            if (state.waitlisted.isEmpty()) EmptyNote("No waitlists", "When an event is full, join its waitlist and it shows here.")
                            else state.waitlisted.forEach { WaitlistCard(it) { onOpen(it.booking.id) } }
                        MyEventsTab.Past -> {
                            if (state.past.isEmpty()) EmptyNote("No previous bookings yet", "Your completed and cancelled events will appear here.")
                            else state.past.forEach { PastCard(it) }
                            SecondaryAction("View attendance history", onClick = onHistory)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyEventsTabs(selected: MyEventsTab, waitlistCount: Int, onTab: (MyEventsTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(colors.outlineVariant).padding(4.dp)) {
        MyEventsTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                onClick = { onTab(tab) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                color = if (isSelected) colors.primary else colors.outlineVariant,
                contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (tab == MyEventsTab.Waitlist && waitlistCount > 0) "${tab.label} ($waitlistCount)" else tab.label, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyBookings(onBrowse: () -> Unit, onEditInterests: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
        Column(
            Modifier.fillMaxWidth().padding(spacing.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Image(painterResource(R.drawable.gilbert), null, Modifier.size(112.dp), contentScale = ContentScale.Fit)
            Text("Your next chapter\nis wide open.", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("No upcoming bookings. Explore campus events to find your next one.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            PrimaryAction("Browse events", onClick = onBrowse)
            TextButton(onClick = onEditInterests) { Text("Manage interests", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun EmptyNote(title: String, body: String) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space5), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BookingCard(card: BookingCardUi, onOpen: () -> Unit, onCheckIn: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val event = card.event
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.testTag("booking_card")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            TagChip("Confirmed", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.CheckCircle)
            Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(event.clubName, style = MaterialTheme.typography.titleSmall)
            Text("${event.whenLabel} · ${event.venue}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            HorizontalDivider(color = colors.outlineVariant)
            if (card.checkInOpen) {
                Text("Check-in is open", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    StatusChip(card.arrival, "${Stage.Arrival.label}: ${card.arrival.label}")
                    if (event.requiresCheckpoint) StatusChip(card.checkpoint, "${Stage.Checkpoint.label}: ${card.checkpoint.label}")
                }
                PrimaryAction("Check in", onClick = onCheckIn)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Icon(Icons.Rounded.AccessTime, null, Modifier.size(18.dp), tint = ScpdTheme.colors.textTertiary)
                    Text(
                        if (event.requiresCheckpoint) "Check-in opens on the day · 2 scans needed" else "Check-in opens on the day",
                        style = MaterialTheme.typography.bodyMedium, color = ScpdTheme.colors.textTertiary,
                    )
                }
            }
            TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
                Text("View booking", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(spacing.space2))
                Icon(Icons.Rounded.NorthEast, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WaitlistCard(card: BookingCardUi, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onOpen, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            TagChip("You're #${card.booking.waitlistPosition} on the waitlist", colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.HourglassTop)
            Text(card.event.title, style = MaterialTheme.typography.titleLarge)
            Text(card.event.whenLabel, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text("We book you automatically if a place opens.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PastCard(card: PastCardUi) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(ScpdTheme.spacing.space4), verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space2)) {
            TagChip(card.status, colors.surfaceVariant, colors.onSurface)
            Text(card.event.title, style = MaterialTheme.typography.titleMedium)
            Text(card.event.whenLabel, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (card.status == "Event cancelled") card.event.cancelReason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingDetails(
    state: MyEventsUiState,
    onBack: () -> Unit,
    onAskCancel: () -> Unit,
    onDismissCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
    onCheckIn: (String) -> Unit,
) {
    val card = state.selected ?: return
    val spacing = ScpdTheme.spacing
    val waitlisted = card.booking.status == BookingStatus.Waitlisted
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShellTopBar(if (waitlisted) "Your waitlist place" else "Your booking", onBack)
        if (state.isOffline) OfflineBanner(body = "You're offline. Cancelling needs a connection.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    state.error?.let { StatusBanner("Not cancelled", body = it, tone = BannerTone.Warning) }
                    Text(
                        if (waitlisted) "You're #${card.booking.waitlistPosition} on the waitlist" else "Your place is booked",
                        Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall,
                    )
                    Box(Modifier.clip(MaterialTheme.shapes.large)) { EventPoster(card.event, featured = true) }
                    Text(card.event.title, style = MaterialTheme.typography.titleLarge)
                    Text(card.event.clubName, style = MaterialTheme.typography.titleSmall)
                    Text("${card.event.whenLabel}\n${card.event.venue}", style = MaterialTheme.typography.bodyLarge)
                    EventTags(card.event.tags)
                    if (!waitlisted) AttendanceRequirement(card.event)
                    if (!card.canCancel && !waitlisted) {
                        Text("Cancellations closed 2 hours before the start. If you can't come, tell the organiser.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        BottomActionBar {
            if (card.checkInOpen) PrimaryAction("Check in") { onCheckIn(card.event.id) }
            if (card.canCancel || waitlisted) {
                SecondaryAction(
                    when {
                        state.cancelling -> "Cancelling…"
                        waitlisted -> "Leave waitlist"
                        else -> "Cancel booking"
                    },
                    onClick = { if (!state.cancelling) onAskCancel() },
                )
            }
        }
    }
    if (state.confirmCancel) {
        ModalBottomSheet(onDismissRequest = onDismissCancel, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            ReadableWidth {
                Column(Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Text(if (waitlisted) "Leave the waitlist?" else "Cancel your place?", style = MaterialTheme.typography.headlineSmall)
                    Text("${card.event.title} · ${card.event.dateLabel}", style = MaterialTheme.typography.titleSmall)
                    Text(if (waitlisted) "You lose your place in the queue." else "Your place goes to the next student on the waitlist.", style = MaterialTheme.typography.bodyLarge)
                    PrimaryAction(if (waitlisted) "Leave waitlist" else "Cancel booking", onClick = onConfirmCancel)
                    SecondaryAction("Keep it", onClick = onDismissCancel)
                    Spacer(Modifier.height(spacing.space4))
                }
            }
        }
    }
}

@Composable
private fun BookingCancelled(state: MyEventsUiState, onBack: () -> Unit, onBrowse: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background)) {
        ShellTopBar("Booking cancelled", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.gilbert), null, Modifier.size(112.dp))
                    TagChip("Booking cancelled", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.CheckCircle)
                    Text("Your place has been released", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    state.cancelledEvent?.let { Text("${it.title} · ${it.dateLabel}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
                    Text("Cancelled bookings earn no points or streak credit.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        BottomActionBar {
            PrimaryAction("Back to My Events", onClick = onBack)
            SecondaryAction("Browse other events", onClick = onBrowse)
        }
    }
}
