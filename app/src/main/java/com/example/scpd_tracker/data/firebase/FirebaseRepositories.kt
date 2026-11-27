package com.example.scpd_tracker.data.firebase

import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.data.ProgressRepository
import com.example.scpd_tracker.data.fake.OfflineException
import com.example.scpd_tracker.domain.BookingRules
import com.example.scpd_tracker.domain.EventForm
import com.example.scpd_tracker.domain.EventFormValidator
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.PointsEntry
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.LocalDate

/**
 * The live Firestore data the student and committee screens share. Each query has one listener for the whole app,
 * restarted when a different account signs in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseStore(val fb: FirebaseHandles, val auth: FirebaseAuthRepository, private val scope: CoroutineScope) {
    private val db = fb.db

    private fun <T> perUser(empty: T, build: (UserProfile) -> Flow<T>): Flow<T> =
        auth.currentUser.map { it?.uid to it }.distinctUntilChanged { a, b -> a.first == b.first }
            .flatMapLatest { (_, user) -> if (user == null) flowOf(empty) else build(user) }
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** Approved events, as raw documents (the session times live there too). */
    val approvedEventDocs: Flow<Map<String, DocumentSnapshot>> = perUser(emptyMap()) {
        db.collection(Collections.EVENTS).whereEqualTo("reviewStatus", ReviewStatus.Approved.name).orderBy("start")
            .snapshots().map { snap -> snap?.documents.orEmpty().associateBy { it.id } }
    }

    val approvedEvents: Flow<Map<String, Event>> =
        approvedEventDocs.map { docs -> docs.mapNotNull { (id, doc) -> doc.toEvent()?.let { id to it } }.toMap() }

    private fun mine(collection: String) = perUser(emptyList<DocumentSnapshot>()) { user ->
        db.collection(collection).whereEqualTo("userId", user.uid).snapshots().map { it?.documents.orEmpty() }
    }

    val myBookingDocs: Flow<List<DocumentSnapshot>> = mine(Collections.BOOKINGS)
    val myAttendanceDocs: Flow<List<DocumentSnapshot>> = mine(Collections.ATTENDANCE)
    val myLedgerDocs: Flow<List<DocumentSnapshot>> = mine(Collections.LEDGER)

    fun eventDoc(id: String): Flow<DocumentSnapshot?> = perUser(null) { db.collection(Collections.EVENTS).document(id).snapshots() }

    /** Bookings and attendance of one event, for its own club only (the rules need the clubId filter). */
    fun clubQuery(collection: String, eventId: String, clubId: String): Query =
        db.collection(collection).whereEqualTo("eventId", eventId).whereEqualTo("clubId", clubId)
}

internal suspend fun <T> online(network: NetworkMonitor, block: suspend () -> T): Result<T> {
    if (!network.isOnline.value) return Result.failure(OfflineException())
    return try {
        Result.success(withTimeout(FIREBASE_TIMEOUT_MS) { block() })
    } catch (e: Throwable) {
        if (e.isNetworkProblem()) Result.failure(OfflineException()) else Result.failure(e)
    }
}

private fun denied(): SecurityException = SecurityException("No access to this club.")

// ---- Events (R1) ---------------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseEventRepository(
    private val store: FirebaseStore,
    private val network: NetworkMonitor,
    private val clock: AppClock,
) : EventRepository {
    private val db = store.fb.db
    private val auth = store.auth

    override fun studentEvents(): Flow<List<Event>> = store.approvedEvents.map { it.values.sortedBy(Event::start) }

    override fun event(id: String): Flow<Event?> = store.eventDoc(id).map { it?.toEvent() }.distinctUntilChanged()

    override fun clubEvents(clubId: String): Flow<List<Event>> = auth.currentUser.flatMapLatest { user ->
        if (user?.role != AccountRole.Committee || clubId !in user.clubIds) flowOf(emptyList())
        else db.collection(Collections.EVENTS).whereEqualTo("clubId", clubId).snapshots()
            .map { snap -> snap?.documents.orEmpty().mapNotNull { it.toEvent() }.sortedBy(Event::start) }
    }

    private fun canManage(clubId: String) = auth.currentUser.value?.let { it.role == AccountRole.Committee && clubId in it.clubIds } ?: false

    private suspend fun eventNow(id: String): Event? = db.collection(Collections.EVENTS).document(id).get().await().toEvent()

    override suspend fun saveDraft(clubId: String, form: EventForm, eventId: String?): Result<String> {
        if (!canManage(clubId)) return Result.failure(denied())
        return online(network) {
            val existing = eventId?.let { eventNow(it) }
            if (existing != null && existing.clubId != clubId) throw denied()
            if (existing != null && !existing.isEditable) {
                throw IllegalStateException("Approved events can't be changed. Cancel and resubmit instead.")
            }
            val start = form.start ?: clock.now().atZone(Policy.ZONE).plusDays(7)
            val content = mapOf(
                "title" to form.title.trim(), "description" to form.description.trim(), "venue" to form.venue.trim(),
                "start" to start.toTimestamp(), "end" to (form.end ?: start.plusHours(2)).toTimestamp(),
                "capacity" to (form.capacity.trim().toIntOrNull()?.coerceIn(0, Policy.MAX_CAPACITY) ?: 0),
                "tags" to form.tags, "attendanceMode" to form.attendanceMode.name, "pointsRequested" to form.pointsRequested,
                "pointsJustification" to form.pointsJustification.trim().ifEmpty { null },
            )
            if (existing != null) {
                db.collection(Collections.EVENTS).document(existing.id).update(content).await()
                existing.id
            } else {
                val club = db.collection(Collections.CLUBS).document(clubId).get().await()
                val ref = db.collection(Collections.EVENTS).document()
                ref.set(content + mapOf(
                    "clubId" to clubId, "clubName" to (club.getString("name") ?: clubId),
                    "organiserContact" to (club.getString("contact") ?: "s-cpd@nottingham.edu.my"),
                    "poster" to "Teal", "featured" to false, "reviewStatus" to ReviewStatus.Draft.name, "reviewComment" to null,
                    "state" to EventState.BookingOpen.name, "bookedCount" to 0, "waitlist" to emptyList<String>(),
                    "createdBy" to auth.requireUid(), "cancelReason" to null, "checkpointOpen" to false,
                    "checkInOpenedAt" to null, "checkInClosedAt" to null, "checkpointOpenedAt" to null,
                    "openedBy" to null, "submittedAt" to null,
                )).await()
                ref.id
            }
        }
    }

    override suspend fun submitForReview(eventId: String): Result<Unit> = online(network) {
        val event = eventNow(eventId) ?: throw NoSuchElementException("Event not found")
        if (!canManage(event.clubId)) throw denied()
        if (!event.isEditable) throw IllegalStateException("Only drafts and events with changes requested can be submitted.")
        val form = EventForm(
            event.title, event.description, event.venue, event.start, event.end, event.capacity.toString(), event.tags,
            event.attendanceMode, event.pointsRequested, event.pointsJustification.orEmpty(),
        )
        EventFormValidator.validate(form, clock.now().atZone(Policy.ZONE)).values.firstOrNull()?.let {
            throw IllegalArgumentException(it.message)
        }
        try {
            db.collection(Collections.EVENTS).document(eventId)
                .update(mapOf("reviewStatus" to ReviewStatus.PendingReview.name, "submittedAt" to FieldValue.serverTimestamp())).await()
        } catch (e: Exception) {
            // The phone clock said it was fine, but the server clock decides the 2-day rule.
            if (e.isPermissionDenied()) throw IllegalArgumentException("Submit at least 2 days before the start, so the s-CPD office can review it.")
            throw e
        }
        Unit
    }

    override suspend fun withdraw(eventId: String): Result<Unit> = online(network) {
        val event = eventNow(eventId) ?: throw NoSuchElementException("Event not found")
        if (!canManage(event.clubId)) throw denied()
        if (event.reviewStatus != ReviewStatus.PendingReview) throw IllegalStateException("Only a pending submission can be withdrawn.")
        db.collection(Collections.EVENTS).document(eventId)
            .update(mapOf("reviewStatus" to ReviewStatus.Draft.name, "submittedAt" to null)).await()
        Unit
    }

    /** Cancelling only changes the event. Each student's app sees it and shows the booking as cancelled by the organiser. */
    override suspend fun cancelEvent(eventId: String, reason: String): Result<Unit> = online(network) {
        val event = eventNow(eventId) ?: throw NoSuchElementException("Event not found")
        if (!canManage(event.clubId)) throw denied()
        if (event.reviewStatus != ReviewStatus.Approved || event.isCancelled) throw IllegalStateException("Only approved events can be cancelled.")
        if (reason.isBlank()) throw IllegalArgumentException("Give students a reason.")
        db.collection(Collections.EVENTS).document(eventId)
            .update(mapOf("state" to EventState.Cancelled.name, "cancelReason" to reason.trim(), "checkpointOpen" to false)).await()
        Unit
    }
}

// ---- Bookings (R2) -------------------------------------------------------------------------------

/** Booked, waitlisted, cancelled or no-show, worked out from the booking, its event and the attendance record. */
internal fun deriveBooking(doc: DocumentSnapshot, eventDoc: DocumentSnapshot?, attendance: DocumentSnapshot?, now: Instant): Booking {
    val eventId = doc.getString("eventId").orEmpty()
    val uid = doc.getString("userId").orEmpty()
    val raw = doc.getString("status")
    val event = eventDoc?.toEvent()
    val active = raw == "booked" || raw == "waitlisted"
    val session = eventDoc?.sessionTimes()
    val deadline = session?.closedAt?.plus(Policy.OFFLINE_UPLOAD_WINDOW) ?: event?.end?.toInstant()?.plus(Policy.OFFLINE_UPLOAD_WINDOW)
    val status = when {
        event?.isCancelled == true && active -> BookingStatus.Cancelled
        raw == "booked" && attendance?.get("arrival") == null && deadline != null && now.isAfter(deadline) -> BookingStatus.NoShow
        raw == "booked" -> BookingStatus.Booked
        raw == "waitlisted" -> BookingStatus.Waitlisted
        raw == "no_show" -> BookingStatus.NoShow
        else -> BookingStatus.Cancelled
    }
    val position = if (status == BookingStatus.Waitlisted) eventDoc?.waitlist()?.indexOf(uid)?.takeIf { it >= 0 }?.plus(1) else null
    return Booking(
        id = doc.id, eventId = eventId, userId = uid, status = status,
        createdAt = doc.getTimestamp("createdAt")?.toInstantValue() ?: Instant.EPOCH,
        waitlistPosition = position, cancelledByOrganiser = event?.isCancelled == true && active,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseBookingRepository(
    private val store: FirebaseStore,
    private val network: NetworkMonitor,
    private val clock: AppClock,
) : BookingRepository {
    private val db = store.fb.db
    private val auth = store.auth

    override fun myBookings(): Flow<List<Booking>> =
        combine(store.myBookingDocs, store.approvedEventDocs, store.myAttendanceDocs) { bookings, events, attendance ->
            val records = attendance.associateBy { it.getString("eventId") }
            bookings.map { deriveBooking(it, events[it.getString("eventId")], records[it.getString("eventId")], clock.now()) }
        }

    override fun myNoShowCount(): Flow<Int> = myBookings().map { list -> list.count { it.status == BookingStatus.NoShow } }

    override suspend fun book(eventId: String, requestId: String) = place(eventId, requestId, waitlist = false)
    override suspend fun joinWaitlist(eventId: String, requestId: String) = place(eventId, requestId, waitlist = true)

    /**
     * One transaction: the booking and the event's count (or waitlist) change together, and the rules refuse
     * anything else. The booking ID is fixed per student and event, so a retry or a double tap can't book twice.
     */
    private suspend fun place(eventId: String, requestId: String, waitlist: Boolean): BookResult {
        if (!network.isOnline.value) return BookResult.Offline
        val user = auth.currentUser.value ?: return BookResult.Rejected(BookingDecision.Closed)
        val now = clock.now().atZone(Policy.ZONE)
        val event = store.approvedEvents.first()[eventId] ?: return BookResult.Rejected(BookingDecision.Closed)
        // Cap and clash are checked here only (rules can't run queries); capacity and timing are checked by the rules.
        val mine = myBookings().first()
        val events = store.approvedEvents.first()
        val active = mine.filter { it.status == BookingStatus.Booked }.mapNotNull { events[it.eventId] }
            .filter { !it.isCancelled && it.end.isAfter(now) }
        val decision = BookingRules.canBook(event, active, mine.count { it.status == BookingStatus.NoShow }, now)
        if (decision != BookingDecision.Allowed && !(decision is BookingDecision.Full && waitlist)) {
            if (decision == BookingDecision.AlreadyBooked) mine.firstOrNull { it.eventId == eventId }?.let { return BookResult.Booked(it) }
            return BookResult.Rejected(decision)
        }
        val eventRef = db.collection(Collections.EVENTS).document(eventId)
        val bookingRef = db.collection(Collections.BOOKINGS).document(pairId(eventId, user.uid))
        return try {
            withTimeout(FIREBASE_TIMEOUT_MS) {
                db.runTransaction { tx ->
                    val eventSnap = tx.get(eventRef)
                    val bookingSnap = tx.get(bookingRef)
                    val status = bookingSnap.getString("status")
                    if (status == "booked") return@runTransaction "booked"
                    if (status == "waitlisted") return@runTransaction "waitlisted"
                    val count = eventSnap.getLong("bookedCount") ?: 0
                    val capacity = eventSnap.getLong("capacity") ?: 0
                    val next = when {
                        count < capacity -> "booked"
                        waitlist -> "waitlisted"
                        else -> return@runTransaction "full"
                    }
                    if (bookingSnap.exists()) {
                        tx.update(bookingRef, mapOf("status" to next, "createdAt" to FieldValue.serverTimestamp(), "requestId" to requestId))
                    } else {
                        tx.set(bookingRef, mapOf(
                            "eventId" to eventId, "userId" to user.uid, "clubId" to event.clubId, "status" to next,
                            "createdAt" to FieldValue.serverTimestamp(), "name" to user.displayName,
                            "studentId" to user.studentId, "requestId" to requestId,
                        ))
                    }
                    if (next == "booked") tx.update(eventRef, "bookedCount", FieldValue.increment(1))
                    else tx.update(eventRef, "waitlist", FieldValue.arrayUnion(user.uid))
                    next
                }.await()
            }.let { outcome ->
                val booking = Booking(bookingRef.id, eventId, user.uid, BookingStatus.Booked, clock.now())
                when (outcome) {
                    "booked" -> BookResult.Booked(booking)
                    "waitlisted" -> BookResult.Waitlisted(booking.copy(status = BookingStatus.Waitlisted))
                    else -> BookResult.Rejected(BookingDecision.Full(canWaitlist = true))
                }
            }
        } catch (e: Exception) {
            when {
                e.isNetworkProblem() -> BookResult.Offline
                // The server clock or a count changed under us: the event started, closed or filled up.
                else -> BookResult.Rejected(BookingDecision.Closed)
            }
        }
    }

    /** Cancelling a place gives it to the first student on the waitlist in the same transaction (the rules check the order). */
    override suspend fun cancel(bookingId: String): Result<Unit> = online(network) {
        val uid = auth.requireUid()
        val bookingRef = db.collection(Collections.BOOKINGS).document(bookingId)
        val eventId = bookingRef.get().await().getString("eventId") ?: throw NoSuchElementException("Booking not found")
        val event = db.collection(Collections.EVENTS).document(eventId).get().await().toEvent()
            ?: throw NoSuchElementException("Event not found")
        val eventRef = db.collection(Collections.EVENTS).document(eventId)
        db.runTransaction { tx ->
            val booking = tx.get(bookingRef)
            val eventSnap = tx.get(eventRef)
            if (booking.getString("userId") != uid) throw NoSuchElementException("Booking not found")
            when (booking.getString("status")) {
                "booked" -> {
                    if (!BookingRules.canCancel(event, clock.now().atZone(Policy.ZONE))) {
                        throw IllegalStateException("Cancellations close 2 hours before the start.")
                    }
                    tx.update(bookingRef, "status", "cancelled")
                    val next = eventSnap.waitlist().firstOrNull()
                    if (next != null) {
                        tx.update(db.collection(Collections.BOOKINGS).document(pairId(eventId, next)), "status", "booked")
                        tx.update(eventRef, "waitlist", FieldValue.arrayRemove(next))
                    } else {
                        tx.update(eventRef, "bookedCount", FieldValue.increment(-1))
                    }
                }
                "waitlisted" -> {
                    tx.update(bookingRef, "status", "cancelled")
                    tx.update(eventRef, "waitlist", FieldValue.arrayRemove(uid))
                }
                else -> Unit
            }
            null
        }.await()
        Unit
    }

    override fun eventBookings(eventId: String): Flow<List<Pair<Booking, UserProfile>>> =
        combine(store.eventDoc(eventId), auth.currentUser) { doc, user -> doc?.getString("clubId")?.takeIf { user?.role == AccountRole.Committee && it in user.clubIds } }
            .distinctUntilChanged()
            .flatMapLatest { clubId ->
                if (clubId == null) flowOf(emptyList())
                else store.clubQuery(Collections.BOOKINGS, eventId, clubId).snapshots().map { snap ->
                    snap?.documents.orEmpty().filter { it.getString("status") != "cancelled" }.map { doc ->
                        val uid = doc.getString("userId").orEmpty()
                        val status = if (doc.getString("status") == "waitlisted") BookingStatus.Waitlisted else BookingStatus.Booked
                        Booking(doc.id, eventId, uid, status, doc.getTimestamp("createdAt")?.toInstantValue() ?: Instant.EPOCH) to
                            UserProfile(uid, doc.getString("name") ?: uid, "", AccountRole.Student, doc.getString("studentId"))
                    }
                }
            }
}

// ---- Points (R5) ---------------------------------------------------------------------------------

/** Ledger reason the app shows while an award is held for office review. */
internal const val HOLD_REASON = "On hold: attendance is under review"

class FirebaseProgressRepository(private val store: FirebaseStore) : ProgressRepository {
    /**
     * The ledger as written on the server, plus a "held" line for any event whose record the office is reviewing.
     * The hold isn't stored: it goes away by itself when the office clears the flag.
     */
    override fun ledger(): Flow<List<PointsEntry>> =
        combine(store.myLedgerDocs, store.myAttendanceDocs) { rows, attendance ->
            val entries = rows.map { doc ->
                PointsEntry(
                    id = doc.id, eventId = doc.getString("eventId").orEmpty(), eventTitle = doc.getString("eventTitle").orEmpty(),
                    date = doc.getString("eventDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(Policy.ZONE),
                    points = doc.getLong("points")?.toInt() ?: 0, reason = doc.getString("reason").orEmpty(),
                )
            }
            val flagged = attendance.filter { it.getString("flagged") != null }.mapNotNull { it.getString("eventId") }.toSet()
            val holds = entries.groupBy { it.eventId }.filterKeys { it in flagged }.mapNotNull { (eventId, list) ->
                val net = list.sumOf { it.points }
                if (net <= 0) null else list.first().copy(id = "hold-$eventId", points = -net, reason = HOLD_REASON)
            }
            (entries + holds).sortedByDescending { it.date }
        }
}
