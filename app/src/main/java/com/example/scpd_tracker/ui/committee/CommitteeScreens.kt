package com.example.scpd_tracker.ui.committee

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.domain.EventFormValidator
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.debug.DebugMenu
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.PillShape
import com.example.scpd_tracker.ui.theme.ScpdTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

// ---- Dashboard ----------------------------------------------------------------------------------

@Composable
internal fun DashboardScreen(state: DashboardUiState, onCreate: () -> Unit, onOpen: (String) -> Unit, onBookings: (String) -> Unit) {
    val spacing = ScpdTheme.spacing
    Column(Modifier.fillMaxSize()) {
        ShellTopBar("Events · ${state.clubName}")
        if (state.isOffline) OfflineBanner(body = "You're offline. Changes and check-in codes need a connection.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    PrimaryAction("Create event", onClick = onCreate)
                    Text("The s-CPD office reviews each event before students can see it. Approved events are locked.", style = MaterialTheme.typography.bodySmall)
                    Section("Changes requested", state.changesRequested) { EventRow(it, "Review feedback") { onOpen(it.id) } }
                    Section("Upcoming", state.upcoming) {
                        EventRow(it, "View bookings", onClick = { onBookings(it.id) }, onManage = { onOpen(it.id) })
                    }
                    Section("Waiting for review", state.pending) { EventRow(it, "View submission") { onOpen(it.id) } }
                    Section("Drafts", state.drafts) { EventRow(it, "Edit draft") { onOpen(it.id) } }
                    Section("Past", state.past) { EventRow(it, "Attendance summary") { onBookings(it.id) } }
                    Section("Rejected or cancelled", state.closed) { EventRow(it, "View") { onOpen(it.id) } }
                    if (!state.loading && listOf(state.upcoming, state.pending, state.drafts, state.past, state.changesRequested).all { it.isEmpty() }) {
                        Text("No events yet. Create your first one.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, events: List<Event>, row: @Composable (Event) -> Unit) {
    if (events.isEmpty()) return
    Text(title, Modifier.semantics { heading() }.padding(top = ScpdTheme.spacing.space3), style = MaterialTheme.typography.titleLarge)
    events.forEach { row(it) }
}

@Composable
private fun ReviewChip(event: Event) {
    val colors = MaterialTheme.colorScheme
    when {
        event.isCancelled -> TagChip("Cancelled", colors.errorContainer, colors.onErrorContainer, Icons.Rounded.Flag)
        event.reviewStatus == ReviewStatus.Approved -> TagChip("Approved · Locked", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.Lock)
        event.reviewStatus == ReviewStatus.PendingReview -> TagChip("Pending review", colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.HourglassTop)
        event.reviewStatus == ReviewStatus.ChangesRequested -> TagChip("Changes requested", colors.errorContainer, colors.onErrorContainer, Icons.Rounded.Edit)
        event.reviewStatus == ReviewStatus.Rejected -> TagChip("Rejected", colors.errorContainer, colors.onErrorContainer, Icons.Rounded.Flag)
        else -> TagChip("Draft", colors.primaryContainer, colors.onSurface, Icons.Rounded.Edit)
    }
}

@Composable
private fun EventRow(event: Event, action: String, onClick: () -> Unit, onManage: (() -> Unit)? = null) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("committee_event")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            ReviewChip(event)
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text("${event.whenLabel} · ${event.venue}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (event.reviewStatus == ReviewStatus.Approved) {
                LinearProgressIndicator(progress = { event.bookedCount / event.capacity.coerceAtLeast(1).toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("${event.bookedCount} / ${event.capacity} booked", style = MaterialTheme.typography.bodySmall)
            }
            event.reviewComment?.let { Text("s-CPD office: $it", style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = colors.primary)
                if (onManage != null) {
                    TextButton(onClick = onManage, modifier = Modifier.testTag("manage_event")) { Text("Manage event") }
                }
            }
        }
    }
}

@Composable
internal fun CheckInHubScreen(state: DashboardUiState, onRun: (String) -> Unit) {
    val spacing = ScpdTheme.spacing
    Column(Modifier.fillMaxSize()) {
        ShellTopBar("Check-in")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Text("Today's events", style = MaterialTheme.typography.titleLarge)
                    if (state.runnable.isEmpty()) Text("No approved events today. Check-in opens on the event day.", style = MaterialTheme.typography.bodyLarge)
                    state.runnable.forEach { event ->
                        EventRow(event, if (event.state == EventState.CheckInOpen) "Show live QR" else "Start check-in") { onRun(event.id) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommitteeAccountScreen(name: String, email: String, onSignOut: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    var debug by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ShellTopBar("Account")
        ReadableWidth {
            Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                TagChip("Committee access verified", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.VerifiedUser)
                Text("You can manage this club's events only. The s-CPD office approves events and corrects attendance.", style = MaterialTheme.typography.bodyMedium)
                ActionRow("Sign out", Icons.AutoMirrored.Rounded.Logout, onSignOut, tint = colors.error)
                if (BuildConfig.DEBUG) ActionRow("Developer menu", Icons.Rounded.BugReport, { debug = true }, supporting = "Play the s-CPD office in the demo")
            }
        }
    }
    if (debug) DebugMenu(onDismiss = { debug = false })
}

// ---- Event form ---------------------------------------------------------------------------------

@Composable
internal fun EventFormScreen(state: EventFormUiState, vm: EventFormViewModel, onBack: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val form = state.form
    val errors = state.errors
    val editable = !state.isLocked && state.status != ReviewStatus.PendingReview
    Column(Modifier.fillMaxSize()) {
        ShellTopBar(if (state.isNew) "Create event" else state.status.label, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    state.error?.let { StatusBanner("Not saved", body = it, tone = BannerTone.Warning) }
                    when {
                        state.isApproved -> StatusBanner("Approved events can't be changed", body = "Cancel and resubmit instead. Booked students are told why.", tone = BannerTone.Info)
                        state.status == ReviewStatus.Rejected -> StatusBanner("Rejected by the s-CPD office", body = state.reviewComment, tone = BannerTone.Warning)
                        state.status == ReviewStatus.ChangesRequested -> StatusBanner("Changes requested", body = state.reviewComment, tone = BannerTone.Warning)
                        state.status == ReviewStatus.PendingReview -> StatusBanner("Waiting for review", body = "Withdraw it to make changes.")
                    }
                    Field("Title", form.title, errors[EventFormValidator.TITLE]?.message, editable, vm::onTitle, "event_title")
                    Field("Description", form.description, errors[EventFormValidator.DESCRIPTION]?.message, editable, vm::onDescription, "event_description", singleLine = false)
                    Field("Venue", form.venue, errors[EventFormValidator.VENUE]?.message, editable, vm::onVenue, "event_venue")
                    DateTimeField("Starts", form.start, errors[EventFormValidator.START]?.message, editable, vm::onStart)
                    DateTimeField("Ends", form.end, errors[EventFormValidator.END]?.message, editable, vm::onEnd)
                    Field("Capacity", form.capacity, errors[EventFormValidator.CAPACITY]?.message, editable, vm::onCapacity, "event_capacity", number = true)
                    Text("Interest tags (1 to 3)", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Policy.INTEREST_CATEGORIES.forEach { tag ->
                            FilterChip(selected = tag in form.tags, onClick = { if (editable) vm.onToggleTag(tag) }, label = { Text(tag) }, shape = PillShape, enabled = editable)
                        }
                    }
                    errors[EventFormValidator.TAGS]?.let { Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text("Attendance", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        FilterChip(form.attendanceMode == AttendanceMode.ArrivalOnly, { if (editable) vm.onMode(AttendanceMode.ArrivalOnly) }, { Text("Arrival scan") }, enabled = editable, shape = PillShape)
                        FilterChip(form.attendanceMode == AttendanceMode.ArrivalAndCheckpoint, { if (editable) vm.onMode(AttendanceMode.ArrivalAndCheckpoint) }, { Text("Arrival + checkpoint") }, enabled = editable, shape = PillShape)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Request s-CPD points", style = MaterialTheme.typography.titleSmall)
                            Text("The office grants 3 points per verified attendee if it approves.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = form.pointsRequested, onCheckedChange = { if (editable) vm.onPointsRequested(it) }, enabled = editable)
                    }
                    if (form.pointsRequested) {
                        Field("Why it earns points", form.pointsJustification, errors[EventFormValidator.JUSTIFICATION]?.message, editable, vm::onJustification, "event_justification", singleLine = false)
                    }
                    if (state.isApproved) CancelEventSection(state, vm)
                }
            }
        }
        BottomActionBar {
            when {
                editable -> {
                    PrimaryAction(if (state.isSaving) "Submitting…" else "Submit for review", enabled = !state.isSaving, onClick = vm::submit)
                    SecondaryAction("Save draft", onClick = vm::saveDraft)
                }
                state.status == ReviewStatus.PendingReview -> SecondaryAction("Withdraw submission", onClick = vm::withdraw)
                else -> SecondaryAction("Back", onClick = onBack)
            }
        }
    }
}

@Composable
private fun CancelEventSection(state: EventFormUiState, vm: EventFormViewModel) {
    val spacing = ScpdTheme.spacing
    var open by rememberSaveable { mutableStateOf(false) }
    HorizontalDivider()
    if (!open) {
        SecondaryAction("Cancel this event") { open = true }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text("Cancel the event?", style = MaterialTheme.typography.titleMedium)
        Text("Bookings close and every booked student sees your reason.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            state.cancelReason, vm::onCancelReason, Modifier.fillMaxWidth().testTag("cancel_reason"),
            label = { Text("Reason for students") }, isError = state.cancelError != null,
            supportingText = { state.cancelError?.let { Text(it) } },
        )
        PrimaryAction("Cancel event", onClick = vm::cancelEvent)
        SecondaryAction("Keep event") { open = false }
    }
}

@Composable
private fun Field(
    label: String, value: String, error: String?, enabled: Boolean, onChange: (String) -> Unit, tag: String,
    singleLine: Boolean = true, number: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) }, enabled = enabled, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        isError = error != null, supportingText = error?.let { { Text(it) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

/** A read-only field that opens a date picker, then a time picker. Times are Malaysia time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(label: String, value: java.time.ZonedDateTime?, error: String?, enabled: Boolean, onPick: (java.time.ZonedDateTime) -> Unit) {
    var step by remember { mutableIntStateOf(0) } // 0 closed, 1 date, 2 time
    var pickedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    OutlinedButton(onClick = { if (enabled) step = 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value?.shortLabel() ?: "Choose date and time", style = MaterialTheme.typography.bodyLarge)
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    if (step == 1) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = (value?.toLocalDate() ?: java.time.LocalDate.now(Policy.ZONE)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    step = if (pickedDate != null) 2 else 0
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } },
        ) { DatePicker(dateState) }
    }
    if (step == 2) {
        val timeState = rememberTimePickerState(initialHour = value?.hour ?: 14, initialMinute = value?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate?.let { onPick(it.atTime(LocalTime.of(timeState.hour, timeState.minute)).atZone(Policy.ZONE)) }
                    step = 0
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } },
            text = { TimePicker(timeState) },
        )
    }
}

// ---- Check-in display ---------------------------------------------------------------------------

@Composable
internal fun CommitteeCheckInScreen(
    state: CommitteeCheckInUiState,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onOpenCheckpoint: () -> Unit,
    onEnd: () -> Unit,
    onAttendees: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val event = state.event
    Column(Modifier.fillMaxSize()) {
        ShellTopBar(event?.title ?: "Check-in", onBack)
        if (state.isOffline) OfflineBanner(body = "No connection: codes paused. Students can't check in until you reconnect.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.fillMaxWidth().padding(spacing.gutter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    state.error?.let { StatusBanner("Couldn't update check-in", body = it, tone = BannerTone.Warning) }
                    when {
                        state.sessionOpen && state.code != null -> {
                            TagChip(if (state.stage == Stage.Checkpoint) "Checkpoint code" else "Arrival code", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            // At most ~260dp, so the code, the count and the actions all fit on a Pixel 2.
                            Surface(color = androidx.compose.ui.graphics.Color.White, shape = MaterialTheme.shapes.large) {
                                QrImage(state.code.qrPayload, Modifier.padding(spacing.space3).widthIn(max = 260.dp).fillMaxWidth(0.75f).aspectRatio(1f).testTag("live_qr"))
                            }
                            Text(state.code.backupCode.chunked(3).joinToString(" "), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("backup_code_display"))
                            Text("New code in ${state.secondsLeft} s", style = MaterialTheme.typography.bodyMedium)
                        }
                        state.sessionOpen -> Text("No connection: codes paused.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        state.sessionClosed -> StatusBanner("Check-in closed", body = "Saved scans can still upload for 2 hours.")
                        else -> Text("Start check-in when students arrive. The QR and code change every 30 seconds.", textAlign = TextAlign.Center)
                    }
                    Text(state.countLabel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("checked_in_count"))
                    if (event?.requiresCheckpoint == true) Text("${state.checkpointCount} at the checkpoint", style = MaterialTheme.typography.bodyMedium)
                    if (state.flaggedCount > 0) TagChip("${state.flaggedCount} flagged for review", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Rounded.Flag)
                    TextButton(onClick = onAttendees) { Text("View attendee list", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        BottomActionBar {
            when {
                state.sessionOpen -> {
                    if (event?.requiresCheckpoint == true && !event.checkpointOpen) PrimaryAction("Open checkpoint", enabled = !state.busy, onClick = onOpenCheckpoint)
                    SecondaryAction("End check-in", onClick = onEnd)
                }
                !state.sessionClosed -> PrimaryAction("Start check-in", enabled = !state.busy && !state.isOffline, onClick = onOpen)
                else -> SecondaryAction("View attendee list", onClick = onAttendees)
            }
        }
    }
}

// ---- Attendees ----------------------------------------------------------------------------------

@Composable
internal fun AttendeesScreen(state: AttendeesUiState, onFilter: (AttendeeFilter) -> Unit, onBack: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val started = state.event?.let { it.state == EventState.CheckInOpen || it.state == EventState.CheckInClosed } == true
    Column(Modifier.fillMaxSize()) {
        ShellTopBar(if (started) "Attendance" else "Bookings", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    state.event?.let { Text("${it.title} · ${it.dateLabel}", style = MaterialTheme.typography.titleMedium) }
                    if (state.waitlisted > 0) Text("${state.waitlisted} on the waitlist", style = MaterialTheme.typography.bodyMedium)
                    if (started) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                            AttendeeFilter.entries.forEach { f ->
                                FilterChip(state.filter == f, { onFilter(f) }, { Text("${f.label} (${state.counts[f] ?: 0})") }, shape = PillShape)
                            }
                        }
                    }
                    if (state.rows.isEmpty()) Text("Nobody here yet.", style = MaterialTheme.typography.bodyLarge)
                    state.rows.forEach { AttendeeCard(it, started, state.event?.requiresCheckpoint == true) }
                }
            }
        }
    }
}

@Composable
private fun AttendeeCard(row: AttendeeRow, started: Boolean, checkpoint: Boolean) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.medium, modifier = Modifier.testTag("attendee_row")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space3), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall)
            Text(row.studentId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (started) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    StatusChip(row.arrival, "Arrival: ${row.arrival.label}")
                    if (checkpoint) StatusChip(row.checkpoint, "Checkpoint: ${row.checkpoint.label}")
                }
            } else {
                TagChip("Booked", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Rounded.CheckCircle)
            }
            row.flagged?.let { TagChip("Flagged: $it", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Rounded.Flag) }
        }
    }
}
