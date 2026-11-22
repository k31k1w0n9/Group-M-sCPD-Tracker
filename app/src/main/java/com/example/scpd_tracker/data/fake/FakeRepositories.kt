package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.data.ProgressRepository
import com.example.scpd_tracker.domain.EventForm
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.CheckInCode
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.PointsEntry
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** Simulated round trip, so loading and "in progress" states are visible in the demo. */
internal const val FAKE_LATENCY_MS = 600L

class FakeAuthRepository(private val backend: FakeBackend, private val network: NetworkMonitor) : AuthRepository {
    private val signedInUid = MutableStateFlow<String?>(null)
    private val user = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = user.asStateFlow()

    override suspend fun signIn(email: String, password: String, requestedRole: AccountRole): SignInResult {
        delay(FAKE_LATENCY_MS)
        if (!network.isOnline.value) return SignInResult.Offline
        val result = backend.signIn(email, password, requestedRole)
        if (result is SignInResult.Success) {
            signedInUid.value = result.user.uid
            user.value = result.user
        }
        return result
    }

    override suspend fun signOut() {
        signedInUid.value = null
        user.value = null
    }

    override suspend fun saveInterests(interests: List<String>) {
        val uid = signedInUid.value ?: return
        backend.saveInterests(uid, interests)
        user.value = backend.user(uid)?.copy(role = user.value?.role ?: AccountRole.Student)
    }

    /** The uid every other repository scopes its reads to. Throws if nobody is signed in. */
    fun requireUid(): String = checkNotNull(signedInUid.value) { "Not signed in" }
    fun uidOrNull(): String? = signedInUid.value
}

class FakeEventRepository(
    private val backend: FakeBackend,
    private val auth: FakeAuthRepository,
    private val network: NetworkMonitor,
) : EventRepository {
    override fun studentEvents(): Flow<List<Event>> =
        backend.events.map { all -> all.values.filter { it.reviewStatus == ReviewStatus.Approved }.sortedBy { it.start } }

    override fun event(id: String): Flow<Event?> = backend.events.map { it[id] }.distinctUntilChanged()

    override fun clubEvents(clubId: String): Flow<List<Event>> =
        combine(backend.events, auth.currentUser) { all, user ->
            if (user?.role == AccountRole.Committee && clubId in user.clubIds) {
                all.values.filter { it.clubId == clubId }.sortedBy { it.start }
            } else emptyList()
        }

    private suspend fun <T> online(block: suspend () -> Result<T>): Result<T> {
        delay(FAKE_LATENCY_MS)
        if (!network.isOnline.value) return Result.failure(OfflineException())
        return block()
    }

    private fun canManage(clubId: String): Boolean = auth.currentUser.value?.let {
        it.role == AccountRole.Committee && clubId in it.clubIds
    } ?: false

    private fun canManageEvent(id: String): Boolean =
        backend.events.value[id]?.clubId?.let(::canManage) ?: false

    private fun <T> denied(): Result<T> = Result.failure(SecurityException("No access to this club."))

    override suspend fun saveDraft(clubId: String, form: EventForm, eventId: String?): Result<String> {
        if (!canManage(clubId)) return denied()
        if (eventId != null && !canManageEvent(eventId)) return denied()
        return online { backend.saveDraft(clubId, form, eventId) }
    }

    override suspend fun submitForReview(eventId: String): Result<Unit> =
        if (canManageEvent(eventId)) online { backend.submitForReview(eventId) } else denied()

    override suspend fun withdraw(eventId: String): Result<Unit> =
        if (canManageEvent(eventId)) online { backend.withdraw(eventId) } else denied()

    override suspend fun cancelEvent(eventId: String, reason: String): Result<Unit> =
        if (canManageEvent(eventId)) online { backend.cancelEvent(eventId, reason) } else denied()
}

class OfflineException : Exception("No connection. Try again when you're back online.")

class FakeBookingRepository(
    private val backend: FakeBackend,
    private val auth: FakeAuthRepository,
    private val network: NetworkMonitor,
) : BookingRepository {
    override fun myBookings(): Flow<List<Booking>> =
        combine(backend.bookings, auth.currentUser) { list, user -> list.filter { it.userId == user?.uid } }

    override fun myNoShowCount(): Flow<Int> = myBookings().map { list -> list.count { it.status == BookingStatus.NoShow } }

    override suspend fun book(eventId: String, requestId: String): BookResult {
        delay(FAKE_LATENCY_MS)
        if (!network.isOnline.value) return BookResult.Offline
        return backend.book(auth.requireUid(), eventId, requestId, waitlist = false)
    }

    override suspend fun joinWaitlist(eventId: String, requestId: String): BookResult {
        delay(FAKE_LATENCY_MS)
        if (!network.isOnline.value) return BookResult.Offline
        return backend.book(auth.requireUid(), eventId, requestId, waitlist = true)
    }

    override suspend fun cancel(bookingId: String): Result<Unit> {
        delay(FAKE_LATENCY_MS)
        if (!network.isOnline.value) return Result.failure(OfflineException())
        return backend.cancelBooking(auth.requireUid(), bookingId)
    }

    override fun eventBookings(eventId: String): Flow<List<Pair<Booking, UserProfile>>> =
        combine(backend.bookings, backend.accounts) { list, _ ->
            list.filter { it.eventId == eventId && it.status != BookingStatus.Cancelled }
                .mapNotNull { b -> backend.user(b.userId)?.let { b to it } }
        }
}

/**
 * The phone side of attendance. Offline scans wait in [queue] (Room's job next week) and upload by themselves
 * when the connection returns. The raw token is dropped only once the server has given a definite answer.
 */
class FakeAttendanceRepository(
    private val backend: FakeBackend,
    private val auth: FakeAuthRepository,
    private val network: NetworkMonitor,
    private val clock: AppClock,
    scope: CoroutineScope,
) : AttendanceRepository {
    private data class Queued(val userId: String, val claim: AttendanceClaim)

    /** What lives on the phone: scans waiting to upload, and server rejections of uploads ("user|event|stage" -> reason). */
    private data class DeviceState(val queue: List<Queued> = emptyList(), val rejectedUploads: Map<String, String> = emptyMap())

    private val device = MutableStateFlow(DeviceState())
    private val lastSynced = MutableStateFlow<Instant?>(null)
    private val syncLock = Mutex()

    init {
        scope.launch {
            network.isOnline.filter { it }.collect { syncPending() }
        }
        // Saved scans belong to the account. Signing out clears them from the phone (the sign-out screen warns first).
        scope.launch {
            var previous: String? = null
            auth.currentUser.collect { user ->
                val left = previous
                if (left != null && user?.uid != left) device.update { s -> s.copy(queue = s.queue.filterNot { it.userId == left }) }
                previous = user?.uid
            }
        }
    }

    override fun myAttendance(): Flow<List<AttendanceRecord>> =
        combine(backend.events, backend.bookings, backend.evidence, device, auth.currentUser) { events, bookings, evidence, state, user ->
            val uid = user?.uid ?: return@combine emptyList()
            val mine = state.queue.filter { it.userId == uid }
            backend.attendanceFor(uid, events, bookings, evidence).map { record ->
                // A scan still on the phone shows as Saved on phone until the server has answered.
                var r = mine.filter { it.claim.eventId == record.eventId }.fold(record) { acc, q ->
                    when (q.claim.stage) {
                        Stage.Arrival -> if (acc.arrival == StageStatus.NOT_SCANNED) acc.copy(arrival = StageStatus.PENDING_SYNC) else acc
                        Stage.Checkpoint -> if (acc.checkpoint == StageStatus.NOT_SCANNED) acc.copy(checkpoint = StageStatus.PENDING_SYNC) else acc
                    }
                }
                // An upload the server refused can't be retried (the token is gone), so it shows as Rejected with the reason.
                Stage.entries.forEach { stage ->
                    val reason = state.rejectedUploads["$uid|${record.eventId}|${stage.name}"] ?: return@forEach
                    r = when (stage) {
                        Stage.Arrival -> if (r.arrival == StageStatus.NOT_SCANNED) r.copy(arrival = StageStatus.REJECTED, rejectReason = reason) else r
                        Stage.Checkpoint -> if (r.checkpoint == StageStatus.NOT_SCANNED) r.copy(checkpoint = StageStatus.REJECTED, rejectReason = reason) else r
                    }
                }
                r
            }
        }

    override fun pendingCount(): Flow<Int> = combine(device, auth.currentUser) { state, user -> state.queue.count { it.userId == user?.uid } }
    override fun lastSyncedAt(): Flow<Instant?> = lastSynced

    override suspend fun submitClaim(claim: AttendanceClaim): ClaimResult {
        val uid = auth.requireUid()
        if (!network.isOnline.value) {
            if (claim.viaBackupCode) return ClaimResult.Rejected("Backup codes need internet. Scan the QR instead, or reconnect.")
            device.update { s -> if (s.queue.any { it.claim.requestId == claim.requestId }) s else s.copy(queue = s.queue + Queued(uid, claim)) }
            return ClaimResult.PendingSync
        }
        delay(FAKE_LATENCY_MS)
        val result = backend.verifyClaim(uid, claim, uploadedLater = false)
        if (result is ClaimResult.Verified || result is ClaimResult.Rejected) lastSynced.value = clock.now()
        return result
    }

    override suspend fun syncPending(): Int = syncLock.withLock {
        var sent = 0
        for (item in device.value.queue) {
            if (!network.isOnline.value) break
            delay(FAKE_LATENCY_MS)
            val result = backend.verifyClaim(item.userId, item.claim, uploadedLater = true)
            if (result != ClaimResult.RateLimited) {
                // A definite answer was stored on the server, so the raw token can leave the phone.
                device.update { s ->
                    val rejected = if (result is ClaimResult.Rejected) {
                        s.rejectedUploads + ("${item.userId}|${item.claim.eventId}|${item.claim.stage.name}" to result.reason)
                    } else s.rejectedUploads
                    s.copy(queue = s.queue.filterNot { it.claim.requestId == item.claim.requestId }, rejectedUploads = rejected)
                }
                sent++
            }
        }
        if (sent > 0) lastSynced.value = clock.now()
        sent
    }

    private suspend fun <T> online(block: () -> Result<T>): Result<T> {
        delay(FAKE_LATENCY_MS / 2)
        if (!network.isOnline.value) return Result.failure(OfflineException())
        return block()
    }

    private fun canManageCheckIn(eventId: String): Boolean = auth.currentUser.value?.let { user ->
        val clubId = backend.events.value[eventId]?.clubId
        user.role == AccountRole.Committee && clubId != null && clubId in user.clubIds
    } ?: false

    private fun deniedCheckIn(): Result<Unit> = Result.failure(SecurityException("No access to this club."))

    override suspend fun openCheckIn(eventId: String) =
        if (canManageCheckIn(eventId)) online { backend.openCheckIn(eventId) } else deniedCheckIn()

    override suspend fun openCheckpoint(eventId: String) =
        if (canManageCheckIn(eventId)) online { backend.openCheckpoint(eventId) } else deniedCheckIn()

    override suspend fun endCheckIn(eventId: String) =
        if (canManageCheckIn(eventId)) online { backend.endCheckIn(eventId) } else deniedCheckIn()

    override suspend fun currentCode(eventId: String, stage: Stage): CheckInCode? =
        if (network.isOnline.value) backend.currentCode(eventId, stage) else null

    override fun liveAttendees(eventId: String): Flow<List<AttendeeRow>> =
        combine(backend.bookings, backend.evidence) { bookings, evidence -> backend.attendeesFor(eventId, bookings, evidence) }
}

class FakeProgressRepository(private val backend: FakeBackend, private val auth: FakeAuthRepository) : ProgressRepository {
    override fun ledger(): Flow<List<PointsEntry>> =
        combine(backend.ledger, auth.currentUser) { rows, user ->
            rows.filter { it.userId == user?.uid }.map { it.entry }.sortedByDescending { it.date }
        }
}
