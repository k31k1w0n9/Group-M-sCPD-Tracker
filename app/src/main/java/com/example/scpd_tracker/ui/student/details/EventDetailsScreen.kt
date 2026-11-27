package com.example.scpd_tracker.ui.student.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun EventDetailsRoute(onBack: () -> Unit, onCheckIn: (String) -> Unit, onMyEvents: () -> Unit) {
    val vm = scpdViewModel { c, s -> EventDetailsViewModel(c.events, c.bookings, c.network, c.clock, s) }
    val state by vm.state.collectAsStateWithLifecycle()
    val user by (LocalContext.current.applicationContext as ScpdApplication)
        .container.auth.currentUser.collectAsStateWithLifecycle()
    ShowMessages(vm.messages.messages)
    // The moment reminders become useful: ask for notifications once, after the first booking that succeeds.
    val notifications = rememberNotificationPermission()
    LaunchedEffect(state.result) {
        if (state.result is BookingOutcomeUi.Confirmed || state.result is BookingOutcomeUi.Waitlisted) {
            notifications.requestAfterFirstBooking()
        }
    }
    EventDetailsScreen(
        state = state, onBack = onBack, memberClubIds = user?.clubIds.orEmpty(),
        onBook = vm::openReview, onConfirm = vm::confirmBooking, onDismissReview = vm::dismissReview,
        onJoinWaitlist = vm::joinWaitlist, onDismissResult = vm::dismissResult,
        onCheckIn = { state.event?.let { onCheckIn(it.id) } }, onMyEvents = onMyEvents,
    )
}

/** Event details: the facts, what attendance needs, the rules, and one booking action pinned at the bottom. */
@Composable
internal fun EventDetailsScreen(
    state: EventDetailsUiState,
    onBack: () -> Unit,
    onBook: () -> Unit,
    onConfirm: () -> Unit,
    onDismissReview: () -> Unit,
    onJoinWaitlist: () -> Unit,
    onDismissResult: () -> Unit,
    onCheckIn: () -> Unit,
    onMyEvents: () -> Unit,
    memberClubIds: List<String> = emptyList(),
) {
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    val event = state.event
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShellTopBar("Event details", onBack)
        if (state.isOffline) OfflineBanner(body = "You're offline. Booking needs a connection.")
        if (event == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator() else Text("This event is no longer available.", style = type.bodyLarge)
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    BookingStatusBanner(state, onDismissResult)
                    Box(Modifier.clip(MaterialTheme.shapes.large)) { EventPoster(event, featured = false) }
                    Text(event.title, Modifier.semantics { heading() }, style = type.headlineSmall)
                    EventTags(event.tags)
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                        Text(event.clubName, style = type.titleSmall)
                        Text(event.whenLabel, style = type.bodyLarge)
                        Text(event.venue, style = type.bodyLarge)
                    }
                    Text(event.description, style = type.bodyLarge)
                    AttendanceRequirement(event)
                    FactRow(Icons.Rounded.Star, eventPointsMessage(event, memberClubIds))
                    FactRow(Icons.Rounded.EventBusy, "Cancel up to 2 hours before the start, so your place can go to someone else.")
                    FactRow(Icons.Rounded.Email, "Questions? Contact the organiser at ${event.organiserContact}")
                }
            }
        }
        BookingBar(state, onBook, onJoinWaitlist, onCheckIn, onMyEvents)
    }
    if (state.showReview) state.event?.let { ReviewBookingSheet(it, state.isSubmitting, onConfirm, onDismissReview) }
}


@Composable
private fun BookingStatusBanner(state: EventDetailsUiState, onDismiss: () -> Unit) {
    val booking = state.myBooking
    when {
        state.result is BookingOutcomeUi.Failed -> StatusBanner("Not booked", body = (state.result as BookingOutcomeUi.Failed).message, tone = BannerTone.Warning)
        booking?.status == BookingStatus.Booked -> StatusBanner("You're booked", body = "It's in My Events. Check in at the event.", tone = BannerTone.Success)
        booking?.status == BookingStatus.Waitlisted ->
            StatusBanner("You're #${booking.waitlistPosition} on the waitlist", body = "We'll book you automatically if a place opens.")
        booking?.status == BookingStatus.Cancelled && booking.cancelledByOrganiser ->
            StatusBanner("Event cancelled by the organiser", body = state.event?.cancelReason, tone = BannerTone.Warning)
    }
}

/** Shown before booking, as Part 1 promised: students see both scans before they commit (R3). */
@Composable
internal fun AttendanceRequirement(event: Event) {
    val colors = MaterialTheme.colorScheme
    val spacing = ScpdTheme.spacing
    Surface(color = colors.tertiaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("attendance_requirement")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = colors.onTertiaryContainer)
                Text(
                    if (event.requiresCheckpoint) "Needs 2 scans" else "Needs 1 scan",
                    style = MaterialTheme.typography.titleMedium, color = colors.onTertiaryContainer,
                )
            }
            Text(
                if (event.requiresCheckpoint) "Scan the QR on arrival, then again at a short checkpoint the organiser opens near the end. Both scans are needed to verify attendance."
                else "Scan the QR on the organiser's screen when you arrive.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("QR scans save offline. Backup codes need internet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun eventPointsMessage(event: Event, memberClubIds: List<String>): String = when {
    event.clubId in memberClubIds -> "Own-club attendance earns 0 s-CPD points, even when verified."
    !event.pointsRequested -> "This event does not award s-CPD points."
    else -> "Earn ${Policy.POINTS_PER_EVENT} s-CPD points once your attendance is verified."
}

@Composable
private fun FactRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookingBar(
    state: EventDetailsUiState,
    onBook: () -> Unit,
    onJoinWaitlist: () -> Unit,
    onCheckIn: () -> Unit,
    onMyEvents: () -> Unit,
) {
    val event = state.event ?: return
    val booking = state.myBooking
    val decision = state.decision
    val supporting = state.blockedReason ?: event.availability(booking?.status == BookingStatus.Booked)
    BottomActionBar(supporting = supporting, modifier = Modifier.testTag("booking_bar")) {
        when {
            booking?.status == BookingStatus.Booked && event.state == EventState.CheckInOpen -> {
                PrimaryAction("Check in", onClick = onCheckIn)
                SecondaryAction("View in My Events", onClick = onMyEvents)
            }
            booking?.status == BookingStatus.Booked -> {
                PrimaryAction("Booked", enabled = false) {}
                SecondaryAction("View in My Events", onClick = onMyEvents)
            }
            booking?.status == BookingStatus.Waitlisted -> PrimaryAction("On the waitlist · #${booking.waitlistPosition}", enabled = false) {}
            decision is BookingDecision.Full ->
                PrimaryAction(if (state.isSubmitting) "Joining…" else "Join waitlist", enabled = !state.isSubmitting, onClick = onJoinWaitlist)
            decision == BookingDecision.Allowed ->
                PrimaryAction(if (state.isSubmitting) "Booking…" else "Book event", enabled = !state.isSubmitting, onClick = onBook)
            else -> PrimaryAction("Can't book", enabled = false) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewBookingSheet(event: Event, submitting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val spacing = ScpdTheme.spacing
    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        ReadableWidth {
            Column(Modifier.padding(horizontal = spacing.gutter, vertical = spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text("Review booking", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                Text("${event.title} · ${event.whenLabel}", style = MaterialTheme.typography.titleSmall)
                AttendanceRequirement(event)
                Text("You can cancel up to 2 hours before the start.", style = MaterialTheme.typography.bodyMedium)
                PrimaryAction(if (submitting) "Booking…" else "Confirm booking", enabled = !submitting, onClick = onConfirm)
                SecondaryAction("Back", onClick = onDismiss)
                Spacer(Modifier.height(spacing.space4))
            }
        }
    }
}
