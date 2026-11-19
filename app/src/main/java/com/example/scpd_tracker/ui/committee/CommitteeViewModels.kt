package com.example.scpd_tracker.ui.committee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.domain.EventForm
import com.example.scpd_tracker.domain.EventFormValidator
import com.example.scpd_tracker.domain.FormError
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.CheckInCode
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.components.MessageQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

// ---- Dashboard ----------------------------------------------------------------------------------

data class DashboardUiState(
    val clubName: String = "",
    val loading: Boolean = true,
    /** Approved events that can run check-in now or later today. */
    val runnable: List<Event> = emptyList(),
    val upcoming: List<Event> = emptyList(),
    val changesRequested: List<Event> = emptyList(),
    val pending: List<Event> = emptyList(),
    val drafts: List<Event> = emptyList(),
    val past: List<Event> = emptyList(),
    val closed: List<Event> = emptyList(),
    val isOffline: Boolean = false,
)

/** A committee only ever sees its own club's events (R6). */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    events: EventRepository,
    auth: AuthRepository,
    network: NetworkMonitor,
    private val clock: AppClock,
) : ViewModel() {
    val state: StateFlow<DashboardUiState> = auth.currentUser.flatMapLatest { user ->
        val clubId = user?.clubIds?.firstOrNull() ?: return@flatMapLatest flowOf(DashboardUiState(loading = false))
        combine(events.clubEvents(clubId), network.isOnline) { list, online ->
            val now = clock.now().atZone(Policy.ZONE)
            val approved = list.filter { it.reviewStatus == ReviewStatus.Approved && !it.isCancelled }
            DashboardUiState(
                clubName = user.displayName,
                loading = false,
                runnable = approved.filter { it.state == EventState.CheckInOpen || (it.end.isAfter(now) && it.start.toLocalDate() == now.toLocalDate()) },
                upcoming = approved.filter { it.end.isAfter(now) },
                changesRequested = list.filter { it.reviewStatus == ReviewStatus.ChangesRequested },
                pending = list.filter { it.reviewStatus == ReviewStatus.PendingReview },
                drafts = list.filter { it.reviewStatus == ReviewStatus.Draft },
                past = approved.filter { !it.end.isAfter(now) }.reversed(),
                closed = list.filter { it.reviewStatus == ReviewStatus.Rejected || it.isCancelled },
                isOffline = !online,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

// ---- Event form (R1) ----------------------------------------------------------------------------

data class EventFormUiState(
    val form: EventForm = EventForm(),
    val errors: Map<String, FormError> = emptyMap(),
    val status: ReviewStatus = ReviewStatus.Draft,
    val reviewComment: String? = null,
    /** Approved or rejected: read-only. An approved event can only be cancelled. */
    val isLocked: Boolean = false,
    val isApproved: Boolean = false,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
    val error: String? = null,
    val finished: Boolean = false,
    val cancelReason: String = "",
    val cancelError: String? = null,
)

/**
 * Create / edit event. Every field lives in SavedStateHandle, so a half-filled form survives rotation and
 * process death. Errors show only after the first submit, then update as the user types.
 */
class EventFormViewModel(
    private val events: EventRepository,
    private val auth: AuthRepository,
    private val clock: AppClock,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val eventId: String? = saved.get<String>("eventId")?.takeIf { it.isNotBlank() && it != "null" }
    private val local = MutableStateFlow(EventFormUiState(isNew = eventId == null))
    val messages = MessageQueue()

    private val existing: Flow<Event?> = eventId?.let { events.event(it) } ?: flowOf(null)

    val state: StateFlow<EventFormUiState> = combine(existing, local, saved.getStateFlow(KEY_FIELDS, emptyMap<String, String>())) { event, l, fields ->
        val form = formFrom(fields)
        l.copy(
            form = form,
            errors = if (fields[SHOW_ERRORS] == "1") EventFormValidator.validate(form, now()) else emptyMap(),
            status = event?.reviewStatus ?: ReviewStatus.Draft,
            reviewComment = event?.reviewComment,
            isLocked = event?.isLocked == true,
            isApproved = event?.reviewStatus == ReviewStatus.Approved && !event.isCancelled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventFormUiState(isNew = eventId == null))

    init {
        // Load the saved event into the form once; after that the form is the source of truth.
        if (eventId != null && saved.get<Boolean>(KEY_LOADED) != true) {
            viewModelScope.launch {
                events.event(eventId).first()?.let {
                    saved[KEY_FIELDS] = fieldsFrom(it)
                    saved[KEY_LOADED] = true
                }
            }
        }
    }

    private fun now(): ZonedDateTime = clock.now().atZone(Policy.ZONE)
    private fun fields(): Map<String, String> = saved[KEY_FIELDS] ?: emptyMap()
    private fun set(key: String, value: String) {
        saved[KEY_FIELDS] = fields() + (key to value)
    }

    fun onTitle(v: String) = set(TITLE, v)
    fun onDescription(v: String) = set(DESCRIPTION, v)
    fun onVenue(v: String) = set(VENUE, v)
    fun onCapacity(v: String) = set(CAPACITY, v.filter(Char::isDigit).take(4))
    fun onStart(v: ZonedDateTime) {
        set(START, v.toString())
        // Keep the end two hours after a new start until the user sets it, so the form stays valid.
        val end = fields()[END]?.let(ZonedDateTime::parse)
        if (end == null || !end.isAfter(v)) set(END, v.plusHours(2).toString())
    }
    fun onEnd(v: ZonedDateTime) = set(END, v.toString())
    fun onToggleTag(tag: String) {
        val tags = formFrom(fields()).tags
        set(TAGS, (if (tag in tags) tags - tag else tags + tag).joinToString(","))
    }
    fun onMode(mode: AttendanceMode) = set(MODE, mode.name)
    fun onPointsRequested(v: Boolean) = set(POINTS, v.toString())
    fun onJustification(v: String) = set(JUSTIFICATION, v)
    fun onCancelReason(v: String) = local.update { it.copy(cancelReason = v, cancelError = null) }

    private fun clubId(): String? = auth.currentUser.value?.clubIds?.firstOrNull()

    fun saveDraft() = persist(submit = false)

    fun submit() {
        set(SHOW_ERRORS, "1")
        if (EventFormValidator.validate(formFrom(fields()), now()).isNotEmpty()) return
        persist(submit = true)
    }

    private fun persist(submit: Boolean) {
        val club = clubId() ?: return
        if (local.value.isSaving || state.value.isLocked) return
        local.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val id = events.saveDraft(club, formFrom(fields()), eventId ?: saved.get<String>(KEY_CREATED_ID))
            val result = id.mapCatching { newId ->
                saved[KEY_CREATED_ID] = newId
                if (submit) events.submitForReview(newId).getOrThrow()
            }
            local.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message, finished = result.isSuccess && submit) }
            if (result.isSuccess) messages.send(if (submit) "Submitted for review. The s-CPD office will reply here." else "Draft saved")
        }
    }

    fun withdraw() {
        val id = eventId ?: return
        viewModelScope.launch {
            val result = events.withdraw(id)
            local.update { it.copy(error = result.exceptionOrNull()?.message) }
            if (result.isSuccess) messages.send("Submission withdrawn. It's a draft again.")
        }
    }

    fun cancelEvent() {
        val id = eventId ?: return
        val reason = local.value.cancelReason.trim()
        if (reason.length < 10) {
            local.update { it.copy(cancelError = "Tell students why, in at least 10 characters.") }
            return
        }
        viewModelScope.launch {
            val result = events.cancelEvent(id, reason)
            local.update { it.copy(cancelError = result.exceptionOrNull()?.message, finished = result.isSuccess) }
            if (result.isSuccess) messages.send("Event cancelled. Booked students are told why.")
        }
    }

    private companion object {
        const val KEY_FIELDS = "form"
        const val KEY_CREATED_ID = "createdId"
        const val KEY_LOADED = "loaded"
        const val TITLE = "title"; const val DESCRIPTION = "description"; const val VENUE = "venue"
        const val START = "start"; const val END = "end"; const val CAPACITY = "capacity"; const val TAGS = "tags"
        const val MODE = "mode"; const val POINTS = "points"; const val JUSTIFICATION = "justification"; const val SHOW_ERRORS = "showErrors"

        fun formFrom(f: Map<String, String>) = EventForm(
            title = f[TITLE].orEmpty(), description = f[DESCRIPTION].orEmpty(), venue = f[VENUE].orEmpty(),
            start = f[START]?.let(ZonedDateTime::parse), end = f[END]?.let(ZonedDateTime::parse), capacity = f[CAPACITY].orEmpty(),
            tags = f[TAGS].orEmpty().split(',').filter { it.isNotBlank() },
            attendanceMode = f[MODE]?.let(AttendanceMode::valueOf) ?: AttendanceMode.ArrivalOnly,
            pointsRequested = f[POINTS]?.toBoolean() ?: true, pointsJustification = f[JUSTIFICATION].orEmpty(),
        )

        fun fieldsFrom(e: Event) = mapOf(
            TITLE to e.title, DESCRIPTION to e.description, VENUE to e.venue, START to e.start.toString(), END to e.end.toString(),
            CAPACITY to e.capacity.toString(), TAGS to e.tags.joinToString(","), MODE to e.attendanceMode.name,
            POINTS to e.pointsRequested.toString(), JUSTIFICATION to e.pointsJustification.orEmpty(),
        )
    }
}

// ---- Committee check-in (R3) --------------------------------------------------------------------

data class CommitteeCheckInUiState(
    val event: Event? = null,
    val stage: Stage = Stage.Arrival,
    val code: CheckInCode? = null,
    val secondsLeft: Int = 0,
    val arrivedCount: Int = 0,
    val checkpointCount: Int = 0,
    val bookedCount: Int = 0,
    val flaggedCount: Int = 0,
    val isOffline: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
) {
    val sessionOpen: Boolean get() = event?.state == EventState.CheckInOpen
    val sessionClosed: Boolean get() = event?.state == EventState.CheckInClosed
    val countLabel: String get() = "$arrivedCount of $bookedCount checked in"
}

/** Shows the live rotating QR and backup code; asks the server for a fresh code every second it is on screen. */
class CommitteeCheckInViewModel(
    events: EventRepository,
    private val attendance: AttendanceRepository,
    network: NetworkMonitor,
    private val clock: AppClock,
    saved: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(saved["eventId"])
    private val local = MutableStateFlow(CommitteeCheckInUiState())

    private val ticker = flow {
        while (true) {
            emit(clock.now())
            delay(1_000)
        }
    }

    val state: StateFlow<CommitteeCheckInUiState> = combine(
        events.event(eventId), attendance.liveAttendees(eventId), network.isOnline, ticker, local,
    ) { event, rows, online, now, l ->
        val stage = if (event?.checkpointOpen == true) Stage.Checkpoint else Stage.Arrival
        val code = if (event?.state == EventState.CheckInOpen) attendance.currentCode(eventId, stage) else null
        l.copy(
            event = event, stage = stage, code = code,
            secondsLeft = code?.let { ((it.expiresAt.epochSecond - now.epochSecond).coerceAtLeast(0)).toInt() } ?: 0,
            arrivedCount = rows.count { it.arrival == StageStatus.VERIFIED },
            checkpointCount = rows.count { it.checkpoint == StageStatus.VERIFIED },
            bookedCount = rows.size, flaggedCount = rows.count { it.flagged != null }, isOffline = !online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommitteeCheckInUiState())

    fun openCheckIn() = run { attendance.openCheckIn(eventId) }
    fun openCheckpoint() = run { attendance.openCheckpoint(eventId) }
    fun endCheckIn() = run { attendance.endCheckIn(eventId) }

    private fun run(block: suspend () -> Result<Unit>) {
        if (local.value.busy) return
        local.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = block()
            local.update { it.copy(busy = false, error = result.exceptionOrNull()?.message) }
        }
    }
}

// ---- Bookings and attendance list ---------------------------------------------------------------

enum class AttendeeFilter(val label: String) { All("All"), CheckedIn("Checked in"), Missing("Not yet"), Flagged("Flagged") }

data class AttendeesUiState(
    val event: Event? = null,
    val rows: List<AttendeeRow> = emptyList(),
    val filter: AttendeeFilter = AttendeeFilter.All,
    val counts: Map<AttendeeFilter, Int> = emptyMap(),
    val waitlisted: Int = 0,
)

class AttendeesViewModel(
    events: EventRepository,
    attendance: AttendanceRepository,
    bookings: BookingRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(saved["eventId"])
    private val filter = saved.getStateFlow(KEY_FILTER, AttendeeFilter.All.name)

    val state: StateFlow<AttendeesUiState> = combine(
        events.event(eventId), attendance.liveAttendees(eventId), bookings.eventBookings(eventId), filter,
    ) { event, rows, all, f ->
        val selected = AttendeeFilter.valueOf(f)
        fun matches(row: AttendeeRow, filter: AttendeeFilter) = when (filter) {
            AttendeeFilter.All -> true
            AttendeeFilter.CheckedIn -> row.arrival == StageStatus.VERIFIED
            AttendeeFilter.Missing -> row.arrival != StageStatus.VERIFIED && row.flagged == null
            AttendeeFilter.Flagged -> row.flagged != null
        }
        AttendeesUiState(
            event = event,
            rows = rows.filter { matches(it, selected) },
            filter = selected,
            counts = AttendeeFilter.entries.associateWith { fl -> rows.count { matches(it, fl) } },
            waitlisted = all.count { it.first.status == BookingStatus.Waitlisted },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttendeesUiState())

    fun onFilter(filter: AttendeeFilter) {
        saved[KEY_FILTER] = filter.name
    }

    private companion object {
        const val KEY_FILTER = "filter"
    }
}
