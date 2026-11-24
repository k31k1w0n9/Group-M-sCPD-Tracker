package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.domain.BookingRules
import com.example.scpd_tracker.domain.EventForm
import com.example.scpd_tracker.domain.EventFormValidator
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.CheckInCode
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.PointsEntry
import com.example.scpd_tracker.model.PosterArt
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/**
 * A pretend server, in memory. It plays the part that Firestore + Cloud Functions take next week:
 * it owns the shared data all three actors see, and it applies the rules the phone must never be trusted with
 * (capacity, signed codes, claim checks, the points ledger). Every method here maps to a future Cloud Function.
 *
 * The signing secret below is only for this demo. In the real build it lives in Secret Manager on the server.
 */
/** Ledger reasons for points held while a record is under review, and given back when the office clears it. */
private const val HOLD_REASON = "On hold: attendance is under review"
private const val RELEASE_REASON = "Review cleared by the office"

class FakeBackend(private val clock: AppClock, seed: FakeSeed = FakeData.seed(clock.now())) {

    data class Account(val profile: UserProfile, val password: String)

    /** Server-side evidence for one student at one event. */
    data class Evidence(
        val arrival: StageStatus = StageStatus.NOT_SCANNED,
        val checkpoint: StageStatus = StageStatus.NOT_SCANNED,
        val rejectReason: String? = null,
        val flagged: String? = null,
    )

    data class Session(val openedAt: Instant, val closedAt: Instant? = null)
    data class LedgerRow(val userId: String, val entry: PointsEntry)

    private val mutex = Mutex()
    private val secret = "scpd-demo-signing-key"

    val accounts = MutableStateFlow(seed.accounts)
    val events = MutableStateFlow(seed.events.associateBy { it.id })
    val bookings = MutableStateFlow(seed.bookings)
    /** Key: "userId|eventId". */
    val evidence = MutableStateFlow(seed.evidence)
    val ledger = MutableStateFlow(seed.ledger)
    val sessions = MutableStateFlow(seed.sessions)
    val alerts = MutableStateFlow<List<BackendAlert>>(emptyList())

    private fun notifyUsers(event: Event, kind: AlertKind, recipients: List<String>, detail: String) {
        val notices = recipients.distinct().map { uid ->
            BackendAlert(UUID.randomUUID().toString(), uid, event.id, event.title,
                event.start.toInstant().toEpochMilli(), event.venue, kind, detail)
        }
        if (notices.isNotEmpty()) alerts.update { it + notices }
    }

    private val processedBookings = mutableMapOf<String, BookResult>()
    private val processedClaims = mutableMapOf<String, ClaimResult>()
    /** Key: "eventId|deviceId" -> the first account that checked in with that phone. */
    private val deviceUse = mutableMapOf<String, String>()
    /** Key: "userId|eventId" -> wrong backup codes this session. */
    private val wrongCodes = mutableMapOf<String, Int>()

    private fun now() = clock.now()
    private fun zonedNow() = now().atZone(Policy.ZONE)
    private fun key(userId: String, eventId: String) = "$userId|$eventId"

    // ---- Accounts -------------------------------------------------------------------------------

    fun signIn(email: String, password: String, requestedRole: AccountRole): SignInResult {
        val account = accounts.value.firstOrNull { it.profile.email.equals(email.trim(), ignoreCase = true) }
        if (account == null || account.password != password) return SignInResult.InvalidCredentials
        if (!account.profile.canUse(requestedRole)) return SignInResult.RoleNotAllowed
        return SignInResult.Success(account.profile.copy(role = requestedRole))
    }

    fun user(uid: String): UserProfile? = accounts.value.firstOrNull { it.profile.uid == uid }?.profile

    fun saveInterests(uid: String, interests: List<String>) {
        accounts.update { list ->
            list.map { if (it.profile.uid == uid) it.copy(profile = it.profile.copy(interests = interests, hasChosenInterests = true)) else it }
        }
    }

    // ---- Events (R1) ----------------------------------------------------------------------------

    private fun updateEvent(id: String, change: (Event) -> Event) = events.update { all ->
        all[id]?.let { all + (id to change(it)) } ?: all
    }

    suspend fun saveDraft(clubId: String, form: EventForm, eventId: String?): Result<String> = mutex.withLock {
        val existing = eventId?.let { events.value[it] }
        if (existing != null && !existing.isEditable) {
            return Result.failure(IllegalStateException("Approved events can't be changed. Cancel and resubmit instead."))
        }
        val id = existing?.id ?: "evt-" + UUID.randomUUID().toString().take(8)
        val clubName = FakeData.clubNames[clubId] ?: clubId
        val start = form.start ?: zonedNow().plusDays(7)
        val event = Event(
            id = id, clubId = clubId, clubName = clubName,
            title = form.title.trim(), description = form.description.trim(),
            start = start, end = form.end ?: start.plusHours(2), venue = form.venue.trim(),
            capacity = form.capacity.trim().toIntOrNull() ?: 0, bookedCount = 0, tags = form.tags,
            reviewStatus = existing?.reviewStatus ?: ReviewStatus.Draft, reviewComment = existing?.reviewComment,
            attendanceMode = form.attendanceMode, pointsRequested = form.pointsRequested,
            pointsJustification = form.pointsJustification.trim().ifEmpty { null },
            organiserContact = FakeData.clubContacts[clubId] ?: "s-cpd@nottingham.edu.my",
            poster = existing?.poster ?: PosterArt.Teal,
        )
        events.update { it + (id to event) }
        Result.success(id)
    }

    suspend fun submitForReview(eventId: String): Result<Unit> = mutex.withLock {
        val event = events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (!event.isEditable) return Result.failure(IllegalStateException("Only drafts and events with changes requested can be submitted."))
        // The server re-checks the form; the phone's validation is only for quick feedback.
        val form = EventForm(
            event.title, event.description, event.venue, event.start, event.end, event.capacity.toString(), event.tags,
            event.attendanceMode, event.pointsRequested, event.pointsJustification.orEmpty(),
        )
        val errors = EventFormValidator.validate(form, zonedNow())
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.values.first().message))
        updateEvent(eventId) { it.copy(reviewStatus = ReviewStatus.PendingReview, submittedAt = zonedNow()) }
        Result.success(Unit)
    }

    suspend fun withdraw(eventId: String): Result<Unit> = mutex.withLock {
        val event = events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (event.reviewStatus != ReviewStatus.PendingReview) return Result.failure(IllegalStateException("Only a pending submission can be withdrawn."))
        updateEvent(eventId) { it.copy(reviewStatus = ReviewStatus.Draft, submittedAt = null) }
        Result.success(Unit)
    }

    suspend fun cancelEvent(eventId: String, reason: String): Result<Unit> = mutex.withLock {
        val event = events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (event.reviewStatus != ReviewStatus.Approved || event.isCancelled) return Result.failure(IllegalStateException("Only approved events can be cancelled."))
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Give students a reason."))
        val affected = bookings.value.filter { it.eventId == eventId &&
            (it.status == BookingStatus.Booked || it.status == BookingStatus.Waitlisted) }.map { it.userId }
        updateEvent(eventId) { it.copy(state = EventState.Cancelled, cancelReason = reason.trim()) }
        bookings.update { list ->
            list.map {
                if (it.eventId == eventId && (it.status == BookingStatus.Booked || it.status == BookingStatus.Waitlisted)) {
                    it.copy(status = BookingStatus.Cancelled, cancelledByOrganiser = true, waitlistPosition = null)
                } else it
            }
        }
        notifyUsers(event, AlertKind.Cancelled, affected, reason.trim())
        Result.success(Unit)
    }

    /** What the s-CPD office does in the backend console (R1). Debug builds expose it in the debug menu. */
    fun adminDecide(eventId: String, decision: ReviewStatus, comment: String?) {
        updateEvent(eventId) { it.copy(reviewStatus = decision, reviewComment = comment) }
    }

    // ---- Bookings (R2) --------------------------------------------------------------------------

    fun noShowCount(uid: String) = bookings.value.count { it.userId == uid && it.status == BookingStatus.NoShow }

    private fun activeEventsOf(uid: String): List<Event> = bookings.value
        .filter { it.userId == uid && it.status == BookingStatus.Booked }
        .mapNotNull { events.value[it.eventId] }
        .filter { !it.isCancelled && it.end.isAfter(zonedNow()) }

    /** One transaction: capacity, clash, cap and pause are checked together, so the last place goes to one student. */
    suspend fun book(uid: String, eventId: String, requestId: String, waitlist: Boolean): BookResult = mutex.withLock {
        processedBookings[requestId]?.let { return it }
        val event = events.value[eventId] ?: return BookResult.Rejected(BookingDecision.Closed)
        val decision = BookingRules.canBook(event, activeEventsOf(uid), noShowCount(uid), zonedNow())
        val alreadyWaiting = bookings.value.firstOrNull { it.userId == uid && it.eventId == eventId && it.status == BookingStatus.Waitlisted }
        val result = when {
            decision == BookingDecision.Allowed -> {
                val booking = Booking(UUID.randomUUID().toString(), eventId, uid, BookingStatus.Booked, now())
                bookings.update { it + booking }
                updateEvent(eventId) { it.copy(bookedCount = it.bookedCount + 1) }
                BookResult.Booked(booking)
            }
            decision is BookingDecision.Full && waitlist -> {
                val booking = alreadyWaiting ?: Booking(
                    UUID.randomUUID().toString(), eventId, uid, BookingStatus.Waitlisted, now(),
                    waitlistPosition = bookings.value.count { it.eventId == eventId && it.status == BookingStatus.Waitlisted } + 1,
                ).also { b -> bookings.update { it + b } }
                BookResult.Waitlisted(booking)
            }
            else -> BookResult.Rejected(decision)
        }
        processedBookings[requestId] = result
        result
    }

    suspend fun cancelBooking(uid: String, bookingId: String): Result<Unit> = mutex.withLock {
        val booking = bookings.value.firstOrNull { it.id == bookingId && it.userId == uid }
            ?: return Result.failure(NoSuchElementException("Booking not found"))
        val event = events.value[booking.eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (booking.status == BookingStatus.Booked && !BookingRules.canCancel(event, zonedNow())) {
            return Result.failure(IllegalStateException("Cancellations close 2 hours before the start."))
        }
        val wasBooked = booking.status == BookingStatus.Booked
        var promotedUser: String? = null
        var list = bookings.value.map { if (it.id == bookingId) it.copy(status = BookingStatus.Cancelled, waitlistPosition = null) else it }
        if (wasBooked) {
            // The freed place goes to the first student on the waitlist, so the count stays the same.
            val next = list.filter { it.eventId == event.id && it.status == BookingStatus.Waitlisted }.minByOrNull { it.createdAt }
            if (next != null) {
                list = list.map { if (it.id == next.id) it.copy(status = BookingStatus.Booked, waitlistPosition = null) else it }
                promotedUser = next.userId
            } else {
                updateEvent(event.id) { it.copy(bookedCount = (it.bookedCount - 1).coerceAtLeast(0)) }
            }
        }
        // Renumber the queue.
        val queue = list.filter { it.eventId == event.id && it.status == BookingStatus.Waitlisted }.sortedBy { it.createdAt }
        list = list.map { b -> queue.indexOfFirst { it.id == b.id }.takeIf { it >= 0 }?.let { b.copy(waitlistPosition = it + 1) } ?: b }
        bookings.value = list
        promotedUser?.let { notifyUsers(event, AlertKind.Waitlist, listOf(it), "You now have a place at this event.") }
        Result.success(Unit)
    }

    // ---- Check-in codes (R3) --------------------------------------------------------------------

    private fun window(at: Instant) = at.epochSecond / Policy.CODE_ROTATION.seconds

    private fun hash(text: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())

    private fun signature(eventId: String, stage: String, window: Long): String =
        hash("$secret|$eventId|$stage|$window").joinToString("") { "%02x".format(it) }.take(16)

    fun backupCode(eventId: String, stage: Stage, window: Long): String {
        val bytes = hash("$secret|code|$eventId|${stage.name}|$window")
        val number = ((bytes[0].toLong() and 0xFF) shl 16) or ((bytes[1].toLong() and 0xFF) shl 8) or (bytes[2].toLong() and 0xFF)
        return (number % 1_000_000).toString().padStart(6, '0')
    }

    fun qrToken(eventId: String, stage: Stage, window: Long) = "SCPD1|$eventId|${stage.name}|$window|${signature(eventId, stage.name, window)}"

    fun openCheckIn(eventId: String): Result<Unit> {
        val event = events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (event.reviewStatus != ReviewStatus.Approved || event.isCancelled) return Result.failure(IllegalStateException("Only approved events can run check-in."))
        updateEvent(eventId) { it.copy(state = EventState.CheckInOpen) }
        sessions.update { it + (eventId to (it[eventId]?.copy(closedAt = null) ?: Session(now()))) }
        return Result.success(Unit)
    }

    fun openCheckpoint(eventId: String): Result<Unit> {
        val event = events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        if (event.state != EventState.CheckInOpen || !event.requiresCheckpoint) return Result.failure(IllegalStateException("Open check-in first."))
        if (event.checkpointOpen) return Result.success(Unit)
        updateEvent(eventId) { it.copy(checkpointOpen = true) }
        val arrived = bookings.value.filter { it.eventId == eventId && it.status == BookingStatus.Booked &&
            evidence.value[key(it.userId, eventId)]?.arrival == StageStatus.VERIFIED }.map { it.userId }
        notifyUsers(event, AlertKind.Checkpoint, arrived, "Scan the checkpoint QR now.")
        return Result.success(Unit)
    }

    fun endCheckIn(eventId: String): Result<Unit> {
        events.value[eventId] ?: return Result.failure(NoSuchElementException("Event not found"))
        updateEvent(eventId) { it.copy(state = EventState.CheckInClosed, checkpointOpen = false) }
        sessions.update { s -> s[eventId]?.let { s + (eventId to it.copy(closedAt = now())) } ?: s }
        return Result.success(Unit)
    }

    fun currentCode(eventId: String, stage: Stage): CheckInCode? {
        val event = events.value[eventId] ?: return null
        if (event.state != EventState.CheckInOpen) return null
        if (stage == Stage.Checkpoint && !event.checkpointOpen) return null
        val w = window(now())
        return CheckInCode(
            qrPayload = qrToken(eventId, stage, w), backupCode = backupCode(eventId, stage, w), stage = stage,
            expiresAt = Instant.ofEpochSecond((w + 1) * Policy.CODE_ROTATION.seconds),
        )
    }

    // ---- Claims (R3, R4) ------------------------------------------------------------------------

    /**
     * Checks one claim. [uploadedLater] is true for scans saved offline: the server can't know when they were
     * captured, so it checks the code was issued during the session and the upload is inside the sync window.
     */
    suspend fun verifyClaim(uid: String, claim: AttendanceClaim, uploadedLater: Boolean): ClaimResult = mutex.withLock {
        processedClaims[claim.requestId]?.let { return it }
        val result = check(uid, claim, uploadedLater)
        // A rate-limit answer is not stored, so the same request can be retried later in a new session.
        if (result != ClaimResult.RateLimited) processedClaims[claim.requestId] = result
        result
    }

    private fun check(uid: String, claim: AttendanceClaim, uploadedLater: Boolean): ClaimResult {
        val event = events.value[claim.eventId] ?: return ClaimResult.Rejected("This event no longer exists.")
        bookings.value.firstOrNull { it.userId == uid && it.eventId == event.id && it.status == BookingStatus.Booked }
            ?: return ClaimResult.Rejected("You don't have a booking for this event.")
        val k = key(uid, event.id)
        val currentWindow = window(now())

        if (claim.viaBackupCode) {
            if (uploadedLater) return ClaimResult.Rejected("Backup codes need internet. Scan the QR instead.")
            if (event.state != EventState.CheckInOpen) return ClaimResult.Rejected("Check-in isn't open for this event.")
            val attempts = wrongCodes[k] ?: 0
            if (attempts >= Policy.BACKUP_CODE_ATTEMPTS) return ClaimResult.RateLimited
            val matches = listOf(currentWindow, currentWindow - 1).any { backupCode(event.id, claim.stage, it) == claim.token.trim() }
            if (!matches) {
                wrongCodes[k] = attempts + 1
                return if (attempts + 1 >= Policy.BACKUP_CODE_ATTEMPTS) ClaimResult.RateLimited
                else ClaimResult.Rejected("Code not recognised or expired. Check the organiser's screen and try again.")
            }
            if (claim.stage == Stage.Checkpoint && !event.checkpointOpen) return ClaimResult.Rejected("The checkpoint isn't open yet.")
        } else {
            val parts = claim.token.split('|')
            if (parts.size != 5 || parts[0] != "SCPD1") return ClaimResult.Rejected("This QR code isn't an s-CPD check-in code.")
            val (_, tokenEvent, tokenStage, windowText, sig) = parts
            val tokenWindow = windowText.toLongOrNull() ?: return ClaimResult.Rejected("This QR code isn't an s-CPD check-in code.")
            if (signature(tokenEvent, tokenStage, tokenWindow) != sig) return ClaimResult.Rejected("This QR code has been altered.")
            if (tokenEvent != event.id) return ClaimResult.Rejected("This QR code is for a different event.")
            if (tokenStage != claim.stage.name) {
                return ClaimResult.Rejected("That's the ${tokenStage.lowercase()} code. Scan the ${claim.stage.label.lowercase()} code.")
            }
            if (!uploadedLater) {
                // The live code plus one rotation of grace, so a scan at the moment the code changes still counts.
                if (currentWindow - tokenWindow > 1 || tokenWindow > currentWindow) {
                    return ClaimResult.Rejected("This QR code has expired. Scan the live code on the organiser's screen.")
                }
            } else {
                val session = sessions.value[event.id] ?: return ClaimResult.Rejected("This event had no check-in session.")
                val issuedAt = Instant.ofEpochSecond(tokenWindow * Policy.CODE_ROTATION.seconds)
                val closed = session.closedAt
                if (issuedAt.isBefore(session.openedAt.minus(Policy.CODE_ROTATION)) || (closed != null && issuedAt.isAfter(closed))) {
                    return ClaimResult.Rejected("This QR code wasn't issued during check-in.")
                }
                if (closed != null && now().isAfter(closed.plus(Policy.OFFLINE_UPLOAD_WINDOW))) {
                    return ClaimResult.Rejected("Uploaded too late. Saved scans must sync within 2 hours of check-in closing.")
                }
            }
        }

        // One phone, one account per event: a second account on the same phone is flagged for the office.
        val owner = deviceUse.getOrPut("${event.id}|${claim.deviceId}") { uid }
        if (owner != uid) {
            val flag = "Same phone used by another account"
            evidence.update { all ->
                all + (k to (all[k] ?: Evidence()).copy(flagged = flag)) +
                    (key(owner, event.id) to (all[key(owner, event.id)] ?: Evidence()).copy(flagged = flag))
            }
            // Points already earned by either account are held until the office decides.
            holdAward(owner, event)
            holdAward(uid, event)
            return ClaimResult.Rejected("Flagged for review: this phone has already checked in another account at this event.")
        }

        evidence.update { all ->
            val current = all[k] ?: Evidence()
            all + (k to if (claim.stage == Stage.Arrival) current.copy(arrival = StageStatus.VERIFIED) else current.copy(checkpoint = StageStatus.VERIFIED))
        }
        awardIfComplete(uid, event)
        return ClaimResult.Verified
    }

    /** Points are written once, together with the outcome, so a retried request can never add a second award (R5). */
    private fun awardIfComplete(uid: String, event: Event) {
        if (user(uid)?.clubIds?.contains(event.clubId) == true) return
        val ev = evidence.value[key(uid, event.id)] ?: return
        if (ev.flagged != null) return // no new points while the office is reviewing
        val complete = ev.arrival == StageStatus.VERIFIED && (!event.requiresCheckpoint || ev.checkpoint == StageStatus.VERIFIED)
        val net = ledger.value.filter { it.userId == uid && it.entry.eventId == event.id }.sumOf { it.entry.points }
        if (complete && event.pointsRequested && net == 0 && ledger.value.none { it.userId == uid && it.entry.eventId == event.id }) {
            ledger.update {
                it + LedgerRow(uid, PointsEntry(UUID.randomUUID().toString(), event.id, event.title, event.start.toLocalDate(), Policy.POINTS_PER_EVENT, "Verified attendance"))
            }
        }
    }

    /** The office reverses an award after finding a record invalid (R5). It happens once and keeps a reason. */
    fun reverseAward(uid: String, eventId: String, reason: String) {
        val event = events.value[eventId] ?: return
        val net = ledger.value.filter { it.userId == uid && it.entry.eventId == eventId }.sumOf { it.entry.points }
        evidence.update { all -> all + (key(uid, eventId) to (all[key(uid, eventId)] ?: Evidence()).copy(arrival = StageStatus.REJECTED, rejectReason = reason, flagged = null)) }
        if (net > 0) {
            ledger.update {
                it + LedgerRow(uid, PointsEntry(UUID.randomUUID().toString(), eventId, event.title, event.start.toLocalDate(), -net, reason))
            }
        }
    }

    /** Points a student already earned for this event are held, so the total is not overstated while it is reviewed. */
    private fun holdAward(uid: String, event: Event) {
        val net = ledger.value.filter { it.userId == uid && it.entry.eventId == event.id }.sumOf { it.entry.points }
        if (net > 0) {
            ledger.update {
                it + LedgerRow(uid, PointsEntry(UUID.randomUUID().toString(), event.id, event.title, event.start.toLocalDate(), -net, HOLD_REASON))
            }
        }
    }

    /** The office decides a flagged record is fine: the flag goes and any held points come back (R5). */
    fun clearFlag(uid: String, eventId: String) {
        val event = events.value[eventId] ?: return
        val k = key(uid, eventId)
        val current = evidence.value[k] ?: return
        if (current.flagged == null) return
        evidence.update { all -> all + (k to current.copy(flagged = null)) }
        val rows = ledger.value.filter { it.userId == uid && it.entry.eventId == eventId }
        val held = rows.any { it.entry.reason == HOLD_REASON }
        val complete = current.arrival == StageStatus.VERIFIED && (!event.requiresCheckpoint || current.checkpoint == StageStatus.VERIFIED)
        if (held && complete && rows.sumOf { it.entry.points } == 0) {
            ledger.update {
                it + LedgerRow(uid, PointsEntry(UUID.randomUUID().toString(), eventId, event.title, event.start.toLocalDate(), Policy.POINTS_PER_EVENT, RELEASE_REASON))
            }
        }
    }

    // ---- Read models ----------------------------------------------------------------------------

    fun attendanceFor(
        uid: String,
        allEvents: Map<String, Event> = events.value,
        allBookings: List<Booking> = bookings.value,
        allEvidence: Map<String, Evidence> = evidence.value,
    ): List<AttendanceRecord> {
        val now = zonedNow()
        return allBookings
            .filter { it.userId == uid && (it.status == BookingStatus.Booked || it.status == BookingStatus.NoShow) }
            .mapNotNull { booking ->
                val event = allEvents[booking.eventId] ?: return@mapNotNull null
                if (event.isCancelled || now.isBefore(event.start) && event.state != EventState.CheckInOpen) return@mapNotNull null
                val ev = allEvidence[key(uid, event.id)] ?: Evidence()
                AttendanceRecord(
                    eventId = event.id, eventTitle = event.title, eventDate = event.start.toLocalDate(),
                    eventEnded = now.isAfter(event.end) || booking.status == BookingStatus.NoShow,
                    requiresCheckpoint = event.requiresCheckpoint, arrival = ev.arrival, checkpoint = ev.checkpoint,
                    tags = event.tags, rejectReason = ev.rejectReason, reviewReason = ev.flagged,
                    // checking in and earning points are separate.
                    pointsEligible = event.pointsRequested && user(uid)?.clubIds?.contains(event.clubId) != true,
                    pointsReason = when {
                        user(uid)?.clubIds?.contains(event.clubId) == true -> "Own-club attendance does not earn points."
                        !event.pointsRequested -> "This event does not award points."
                        else -> null
                    },
                )
            }
            .sortedByDescending { it.eventDate }
    }

    fun attendeesFor(eventId: String, allBookings: List<Booking>, allEvidence: Map<String, Evidence>): List<AttendeeRow> =
        allBookings.filter { it.eventId == eventId && it.status == BookingStatus.Booked }.map { booking ->
            val profile = user(booking.userId)
            val ev = allEvidence[key(booking.userId, eventId)] ?: Evidence()
            AttendeeRow(
                userId = booking.userId, name = profile?.displayName ?: booking.userId, studentId = profile?.studentId.orEmpty(),
                arrival = ev.arrival, checkpoint = ev.checkpoint, flagged = ev.flagged,
            )
        }.sortedBy { it.name }
}
