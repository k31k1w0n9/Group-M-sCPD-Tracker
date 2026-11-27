package com.example.scpd_tracker.data.firebase

import android.content.Context
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.data.fake.OfflineException
import com.example.scpd_tracker.data.local.PendingScan
import com.example.scpd_tracker.data.local.RejectedUpload
import com.example.scpd_tracker.data.local.ScanQueueDao
import com.example.scpd_tracker.data.local.ScanSyncWorker
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.CheckInCode
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val SAME_PHONE = "Same phone used by another account"
private const val OTHER_PHONE = "Checked in from a phone this account isn't set up on"

/**
 * Attendance on the free plan. The phone writes each stage itself and the Firestore rules decide whether it
 * counts: they re-compute the QR signature from the session secret and check the window against the server clock.
 *
 * Offline scans wait in Room ([ScanQueueDao]) and WorkManager ([ScanSyncWorker]) uploads them once the phone is
 * online, even if the app was closed. A scan leaves the phone only after the server gave a definite answer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAttendanceRepository(
    private val context: Context,
    private val store: FirebaseStore,
    private val queue: ScanQueueDao,
    private val network: NetworkMonitor,
    private val clock: AppClock,
    scope: CoroutineScope,
) : AttendanceRepository {
    private val db = store.fb.db
    private val auth = store.auth
    private val lastSynced = MutableStateFlow<Instant?>(null)
    private val syncLock = Mutex()

    /** Committee side: the session secret, and how far this phone's clock is from the server's (server - phone). */
    private val secrets = ConcurrentHashMap<String, String>()
    private val clockOffsets = ConcurrentHashMap<String, Duration>()

    init {
        scope.launch { network.isOnline.filter { it }.collect { syncPending() } }
        // Saved scans belong to the account. Signing out clears them (the sign-out screen warns first).
        scope.launch {
            var previous: String? = null
            auth.currentUser.collect { user ->
                val left = previous
                if (left != null && user?.uid != left) {
                    queue.clearPending(left)
                    queue.clearRejected(left)
                }
                previous = user?.uid
                if (user != null) ScanSyncWorker.schedule(context)
            }
        }
    }

    // ---- Student: reading ------------------------------------------------------------------------

    private val pendingScans: Flow<List<PendingScan>> =
        auth.currentUser.flatMapLatest { u -> if (u == null) flowOf(emptyList()) else queue.pending(u.uid) }
    private val rejectedUploads: Flow<List<RejectedUpload>> =
        auth.currentUser.flatMapLatest { u -> if (u == null) flowOf(emptyList()) else queue.rejected(u.uid) }

    override fun myAttendance(): Flow<List<AttendanceRecord>> = combine(
        combine(store.myBookingDocs, store.approvedEventDocs, store.myAttendanceDocs, ::Triple),
        pendingScans, rejectedUploads, auth.currentUser,
    ) { (bookings, events, records), pending, rejected, user ->
        val uid = user?.uid ?: return@combine emptyList()
        val now = clock.now()
        val byEvent = records.associateBy { it.getString("eventId") }
        bookings.mapNotNull { bookingDoc ->
            val eventId = bookingDoc.getString("eventId") ?: return@mapNotNull null
            val eventDoc = events[eventId] ?: return@mapNotNull null
            val event = eventDoc.toEvent() ?: return@mapNotNull null
            val record = byEvent[eventId]
            val booking = deriveBooking(bookingDoc, eventDoc, record, now)
            if (booking.status != BookingStatus.Booked && booking.status != BookingStatus.NoShow) return@mapNotNull null
            val nowZoned = now.atZone(Policy.ZONE)
            if (nowZoned.isBefore(event.start) && event.state != EventState.CheckInOpen) return@mapNotNull null
            val ownClub = event.clubId in user.clubIds
            val officeReject = record?.getString("rejectReason")
            var r = AttendanceRecord(
                eventId = eventId, eventTitle = event.title, eventDate = event.start.toLocalDate(),
                eventEnded = nowZoned.isAfter(event.end) || booking.status == BookingStatus.NoShow,
                requiresCheckpoint = event.requiresCheckpoint,
                arrival = when {
                    officeReject != null -> StageStatus.REJECTED
                    record?.get("arrival") != null -> StageStatus.VERIFIED
                    else -> StageStatus.NOT_SCANNED
                },
                checkpoint = if (record?.get("checkpoint") != null) StageStatus.VERIFIED else StageStatus.NOT_SCANNED,
                tags = event.tags, rejectReason = officeReject, reviewReason = record?.getString("flagged"),
                pointsEligible = event.pointsRequested && !ownClub,
                pointsReason = when {
                    ownClub -> "Own-club attendance does not earn points."
                    !event.pointsRequested -> "This event does not award points."
                    else -> null
                },
            )
            // A scan still on the phone shows as Saved on phone until the server has answered.
            pending.filter { it.eventId == eventId }.forEach { scan ->
                r = when (Stage.valueOf(scan.stage)) {
                    Stage.Arrival -> if (r.arrival == StageStatus.NOT_SCANNED) r.copy(arrival = StageStatus.PENDING_SYNC) else r
                    Stage.Checkpoint -> if (r.checkpoint == StageStatus.NOT_SCANNED) r.copy(checkpoint = StageStatus.PENDING_SYNC) else r
                }
            }
            // An upload the server refused can't be retried (the token is gone), so it shows as Rejected with the reason.
            rejected.filter { it.eventId == eventId }.forEach { rej ->
                r = when (Stage.valueOf(rej.stage)) {
                    Stage.Arrival -> if (r.arrival == StageStatus.NOT_SCANNED) r.copy(arrival = StageStatus.REJECTED, rejectReason = rej.reason) else r
                    Stage.Checkpoint -> if (r.checkpoint == StageStatus.NOT_SCANNED) r.copy(checkpoint = StageStatus.REJECTED, rejectReason = rej.reason) else r
                }
            }
            r
        }.sortedByDescending { it.eventDate }
    }

    override fun pendingCount(): Flow<Int> = pendingScans.map { it.size }
    override fun lastSyncedAt(): Flow<Instant?> = lastSynced

    // ---- Student: claiming -----------------------------------------------------------------------

    override suspend fun submitClaim(claim: AttendanceClaim): ClaimResult {
        val uid = auth.requireUid()
        if (!network.isOnline.value) {
            if (claim.viaBackupCode) return ClaimResult.Rejected("Backup codes need internet. Scan the QR instead, or reconnect.")
            queue.add(PendingScan.from(uid, claim))
            ScanSyncWorker.schedule(context)
            return ClaimResult.PendingSync
        }
        val result = try {
            withTimeout(FIREBASE_TIMEOUT_MS) { upload(uid, claim, offline = false) }
        } catch (e: Exception) {
            if (!e.isNetworkProblem() || claim.viaBackupCode) throw e
            // The connection dropped mid-claim: keep the scan on the phone instead of losing it.
            queue.add(PendingScan.from(uid, claim))
            ScanSyncWorker.schedule(context)
            return ClaimResult.PendingSync
        }
        if (result is ClaimResult.Verified || result is ClaimResult.Rejected) lastSynced.value = clock.now()
        return result
    }

    override suspend fun syncPending(): Int = syncLock.withLock {
        val uid = auth.uidOrNull() ?: return@withLock 0
        var sent = 0
        for (scan in queue.pendingNow(uid)) {
            if (!network.isOnline.value) break
            val result = try {
                withTimeout(FIREBASE_TIMEOUT_MS) { upload(uid, scan.toClaim(), offline = true) }
            } catch (e: Exception) {
                if (e.isNetworkProblem()) break else ClaimResult.Rejected("This scan couldn't be checked. Ask the organiser.")
            }
            if (result is ClaimResult.Rejected) {
                queue.addRejected(RejectedUpload("$uid|${scan.eventId}|${scan.stage}", uid, scan.eventId, scan.stage, result.reason))
            }
            // A definite answer was stored on the server, so the raw token can leave the phone.
            queue.remove(scan.requestId)
            sent++
        }
        if (sent > 0) lastSynced.value = clock.now()
        runCatching { reconcileAwards(uid) }
        sent
    }

    /** Checks one claim against the rules by writing it. Returns a friendly reason when the rules refuse. */
    private suspend fun upload(uid: String, claim: AttendanceClaim, offline: Boolean): ClaimResult {
        val eventRef = db.collection(Collections.EVENTS).document(claim.eventId)
        val eventSnap = eventRef.get().await()
        val event = eventSnap.toEvent() ?: return ClaimResult.Rejected("This event no longer exists.")
        val booking = db.collection(Collections.BOOKINGS).document(pairId(event.id, uid)).get().await()
        if (booking.getString("status") != "booked") return ClaimResult.Rejected("You don't have a booking for this event.")
        val stageKey = SessionCodes.stageKey(claim.stage)

        val windows: List<Long>
        val stageFor: (Long) -> Map<String, Any?>
        if (claim.viaBackupCode) {
            if (offline) return ClaimResult.Rejected("Backup codes need internet. Scan the QR instead.")
            if (event.state != EventState.CheckInOpen) return ClaimResult.Rejected("Check-in isn't open for this event.")
            if (claim.stage == Stage.Checkpoint && !event.checkpointOpen) return ClaimResult.Rejected("The checkpoint isn't open yet.")
            val code = claim.token.trim()
            if (!code.matches(Regex("[0-9]{6}"))) return ClaimResult.Rejected("Enter the 6-digit code from the organiser's screen.")
            if (!registerAttempt(uid, event.id, code)) return ClaimResult.RateLimited
            // The phone doesn't know the secret, so it tries the current window, then the one before it.
            val current = SessionCodes.windowAt(clock.now())
            windows = listOf(current, current - 1)
            stageFor = { w -> mapOf("method" to "code", "window" to w, "code" to code, "offline" to false,
                "receivedAt" to FieldValue.serverTimestamp(), "requestId" to claim.requestId) }
        } else {
            val qr = SessionCodes.parse(claim.token) ?: return ClaimResult.Rejected("This QR code isn't an s-CPD check-in code.")
            if (qr.eventId != event.id) return ClaimResult.Rejected("This QR code is for a different event.")
            if (qr.stage != stageKey) return ClaimResult.Rejected("That's the ${qr.stage} code. Scan the ${claim.stage.label.lowercase()} code.")
            windows = listOf(qr.window)
            stageFor = { w -> mapOf("method" to "qr", "window" to w, "sig" to qr.signature, "offline" to offline,
                "receivedAt" to FieldValue.serverTimestamp(), "requestId" to claim.requestId) }
        }

        val recordRef = db.collection(Collections.ATTENDANCE).document(pairId(event.id, uid))
        val record = recordRef.get().await()
        if (record.get(stageKey) != null) return ClaimResult.Verified // already saved: a retry is fine

        val boundDevice = db.collection(Collections.USERS).document(uid).get().await().getString("deviceId")
        val useRef = db.collection(Collections.DEVICE_USE).document(pairId(event.id, claim.deviceId))
        val use = useRef.get().await()
        val phoneOwner = use.getString("uid")
        val flag = record.getString("flagged") ?: when {
            phoneOwner != null && phoneOwner != uid -> SAME_PHONE
            boundDevice != claim.deviceId -> OTHER_PHONE
            else -> null
        }

        var accepted = false
        for (w in windows) {
            accepted = commitStage(uid, event.id, event.clubId, claim.deviceId, stageKey, stageFor(w), record.exists(),
                createUse = !use.exists(), flag = flag, flagOwner = phoneOwner?.takeIf { flag == SAME_PHONE && it != uid })
            if (accepted) break
        }
        if (!accepted) return ClaimResult.Rejected(explainRefusal(eventSnap, claim, windows.first(), offline))
        runCatching { reconcileAwards(uid) }
        return when (flag) {
            null -> ClaimResult.Verified
            SAME_PHONE -> ClaimResult.Rejected("Flagged for review: this phone has already checked in another account at this event.")
            else -> ClaimResult.Rejected("Saved for review: this account is set up on a different phone. The s-CPD office will check it.")
        }
    }

    /** Writes the stage. Returns false when the rules refused it. */
    private suspend fun commitStage(
        uid: String, eventId: String, clubId: String, deviceId: String, stageKey: String, stage: Map<String, Any?>,
        recordExists: Boolean, createUse: Boolean, flag: String?, flagOwner: String?,
    ): Boolean {
        suspend fun attempt(withOwnerFlag: Boolean): Boolean = try {
            val batch = db.batch()
            if (createUse) {
                batch.set(db.collection(Collections.DEVICE_USE).document(pairId(eventId, deviceId)),
                    mapOf("eventId" to eventId, "deviceId" to deviceId, "uid" to uid, "at" to FieldValue.serverTimestamp()))
            }
            val ref = db.collection(Collections.ATTENDANCE).document(pairId(eventId, uid))
            if (recordExists) batch.update(ref, mapOf(stageKey to stage, "flagged" to flag, "deviceId" to deviceId))
            else batch.set(ref, mapOf("eventId" to eventId, "userId" to uid, "clubId" to clubId, "deviceId" to deviceId,
                stageKey to stage, "flagged" to flag))
            // The first account on this phone is flagged too, so neither earns points until the office decides.
            if (withOwnerFlag && flagOwner != null) {
                batch.update(db.collection(Collections.ATTENDANCE).document(pairId(eventId, flagOwner)), "flagged", flag)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            if (e.isPermissionDenied()) false else throw e
        }
        // If the other record was already flagged (or gone), save this one on its own.
        return attempt(withOwnerFlag = true) || (flagOwner != null && attempt(withOwnerFlag = false))
    }

    /** Counts a backup-code try on the server before it is checked. False once the 5 tries are used up. */
    private suspend fun registerAttempt(uid: String, eventId: String, code: String): Boolean {
        val ref = db.collection(Collections.ATTEMPTS).document(pairId(eventId, uid))
        val count = ref.get().await().getLong("count") ?: 0
        if (count >= Policy.BACKUP_CODE_ATTEMPTS) return false
        return try {
            if (count == 0L) ref.set(mapOf("eventId" to eventId, "count" to 1, "lastCode" to code, "at" to FieldValue.serverTimestamp())).await()
            else ref.update(mapOf("count" to count + 1, "lastCode" to code, "at" to FieldValue.serverTimestamp())).await()
            true
        } catch (e: Exception) {
            if (e.isPermissionDenied()) false else throw e
        }
    }

    /** The rules only say yes or no, so the phone works out the likely reason from what it knows. */
    private fun explainRefusal(eventSnap: DocumentSnapshot, claim: AttendanceClaim, window: Long, offline: Boolean): String {
        val event = eventSnap.toEvent()
        val session = eventSnap.sessionTimes()
        val now = clock.now()
        val issued = SessionCodes.windowStart(window)
        return when {
            claim.viaBackupCode -> "Code not recognised or expired. Check the organiser's screen and try again."
            session.openedAt == null -> "This event had no check-in session."
            offline && session.closedAt != null && now.isAfter(session.closedAt.plus(Policy.OFFLINE_UPLOAD_WINDOW)) ->
                "Uploaded too late. Saved scans must sync within 2 hours of check-in closing."
            offline && (issued.isBefore(session.openedAt.minus(Policy.CODE_ROTATION)) ||
                (session.closedAt != null && issued.isAfter(session.closedAt))) -> "This QR code wasn't issued during check-in."
            !offline && event?.state != EventState.CheckInOpen -> "Check-in isn't open for this event."
            !offline && claim.stage == Stage.Checkpoint && event?.checkpointOpen != true -> "The checkpoint isn't open yet."
            !offline && Duration.between(issued, now) > Policy.CODE_ROTATION.multipliedBy(2) ->
                "This QR code has expired. Scan the live code on the organiser's screen."
            else -> "This QR code has been altered."
        }
    }

    /**
     * Writes the +3 for every complete, unflagged, eligible record that has no award yet. The rules allow each award
     * once (fixed ID). This also pays out points the office released by clearing a flag.
     */
    private suspend fun reconcileAwards(uid: String) {
        val user = auth.currentUser.value ?: return
        if (user.role != AccountRole.Student && user.studentId == null) return
        val awarded = db.collection(Collections.LEDGER).whereEqualTo("userId", uid).get().await().documents
            .mapNotNull { it.getString("eventId") }.toSet()
        val records = db.collection(Collections.ATTENDANCE).whereEqualTo("userId", uid).get().await().documents
        for (record in records) {
            val eventId = record.getString("eventId") ?: continue
            if (eventId in awarded || record.getString("flagged") != null || record.get("arrival") == null) continue
            val event = db.collection(Collections.EVENTS).document(eventId).get().await().toEvent() ?: continue
            if (!event.pointsRequested || event.clubId in user.clubIds) continue
            if (event.requiresCheckpoint && record.get("checkpoint") == null) continue
            runCatching {
                db.collection(Collections.LEDGER).document("${pairId(eventId, uid)}_award").set(mapOf(
                    "userId" to uid, "eventId" to eventId, "eventTitle" to event.title,
                    "eventDate" to event.start.toLocalDate().toString(), "points" to Policy.POINTS_PER_EVENT,
                    "reason" to "Verified attendance", "createdAt" to FieldValue.serverTimestamp(),
                )).await()
            }
        }
    }

    // ---- Committee -------------------------------------------------------------------------------

    private fun canManage(clubId: String?) = auth.currentUser.value?.let {
        it.role == AccountRole.Committee && clubId != null && clubId in it.clubIds
    } ?: false

    private suspend fun eventSnap(eventId: String, cacheFirst: Boolean = false): DocumentSnapshot? =
        freshOrCached(db.collection(Collections.EVENTS).document(eventId), allowCache = cacheFirst)

    /**
     * The server's copy when online, so a stale cached copy (an old session secret, a checkpoint that has since
     * opened) is never used by mistake. The on-disk cache only when offline, so the committee phone keeps working.
     */
    private suspend fun freshOrCached(ref: com.google.firebase.firestore.DocumentReference, allowCache: Boolean): DocumentSnapshot? {
        if (network.isOnline.value || !allowCache) {
            runCatching { withTimeout(FIREBASE_TIMEOUT_MS) { ref.get(Source.SERVER).await() } }.getOrNull()?.let { return it }
            if (!allowCache) return null
        }
        return runCatching { ref.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() }
    }

    /**
     * Opens check-in. The phone makes the session secret (or reuses this event's one) and writes it together with
     * the event change, and the rules refuse to open check-in without it. The secret stays on this phone, so codes
     * keep refreshing if the committee loses its connection.
     */
    override suspend fun openCheckIn(eventId: String): Result<Unit> {
        val snap = eventSnap(eventId) ?: return Result.failure(OfflineException())
        if (!canManage(snap.getString("clubId"))) return Result.failure(SecurityException("No access to this club."))
        return online(network) {
            val uid = auth.requireUid()
            val secretRef = db.collection(Collections.SECRETS).document(eventId)
            val secret = secretRef.get().await().getString("secret") ?: SessionCodes.newSecret()
            if (snap.getString("state") != EventState.CheckInOpen.name) {
                val before = Instant.now()
                val batch = db.batch()
                batch.set(secretRef, mapOf("secret" to secret, "createdBy" to uid))
                batch.update(db.collection(Collections.EVENTS).document(eventId), mapOf(
                    "state" to EventState.CheckInOpen.name,
                    "checkInOpenedAt" to (snap.getTimestamp("checkInOpenedAt") ?: FieldValue.serverTimestamp()),
                    "checkInClosedAt" to null, "openedBy" to uid,
                ))
                batch.commit().await()
                // Measure this phone's clock against the server's, so offline codes stay in step with the rules.
                if (snap.getTimestamp("checkInOpenedAt") == null) {
                    val serverOpened = db.collection(Collections.EVENTS).document(eventId).get(Source.SERVER).await()
                        .getTimestamp("checkInOpenedAt")?.toInstantValue()
                    if (serverOpened != null) {
                        val local = before.plus(Duration.between(before, Instant.now()).dividedBy(2))
                        clockOffsets[eventId] = Duration.between(local, serverOpened)
                    }
                }
            }
            secrets[eventId] = secret
            Unit
        }
    }

    override suspend fun openCheckpoint(eventId: String): Result<Unit> {
        val snap = eventSnap(eventId) ?: return Result.failure(OfflineException())
        if (!canManage(snap.getString("clubId"))) return Result.failure(SecurityException("No access to this club."))
        val event = snap.toEvent() ?: return Result.failure(NoSuchElementException("Event not found"))
        if (event.state != EventState.CheckInOpen || !event.requiresCheckpoint) return Result.failure(IllegalStateException("Open check-in first."))
        if (event.checkpointOpen) return Result.success(Unit)
        return online(network) {
            db.collection(Collections.EVENTS).document(eventId)
                .update(mapOf("checkpointOpen" to true, "checkpointOpenedAt" to FieldValue.serverTimestamp())).await()
            Unit
        }
    }

    override suspend fun endCheckIn(eventId: String): Result<Unit> {
        val snap = eventSnap(eventId) ?: return Result.failure(OfflineException())
        if (!canManage(snap.getString("clubId"))) return Result.failure(SecurityException("No access to this club."))
        if (snap.getString("state") != EventState.CheckInOpen.name) return Result.success(Unit)
        return online(network) {
            db.collection(Collections.EVENTS).document(eventId).update(mapOf(
                "state" to EventState.CheckInClosed.name, "checkpointOpen" to false,
                "checkInClosedAt" to FieldValue.serverTimestamp(),
            )).await()
            Unit
        }
    }

    /** Made on this phone from the session secret, so it keeps working offline once check-in is open. */
    override suspend fun currentCode(eventId: String, stage: Stage): CheckInCode? {
        val snap = eventSnap(eventId, cacheFirst = true) ?: return null
        if (!canManage(snap.getString("clubId"))) return null
        val event = snap.toEvent() ?: return null
        if (event.state != EventState.CheckInOpen) return null
        if (stage == Stage.Checkpoint && !event.checkpointOpen) return null
        val secret = secrets[eventId]
            ?: freshOrCached(db.collection(Collections.SECRETS).document(eventId), allowCache = true)?.getString("secret")
                ?.also { secrets[eventId] = it }
            ?: return null
        val offset = clockOffsets[eventId] ?: Duration.ZERO
        val serverNow = clock.now().plus(offset)
        val window = SessionCodes.windowAt(serverNow)
        return CheckInCode(
            qrPayload = SessionCodes.qrPayload(secret, eventId, stage, window),
            backupCode = SessionCodes.backupCode(secret, eventId, stage, window),
            stage = stage,
            expiresAt = SessionCodes.windowStart(window + 1).minus(offset),
        )
    }

    override fun liveAttendees(eventId: String): Flow<List<AttendeeRow>> =
        combine(store.eventDoc(eventId), auth.currentUser) { doc, _ -> doc?.getString("clubId")?.takeIf(::canManage) }
            .distinctUntilChanged()
            .flatMapLatest { clubId ->
                if (clubId == null) flowOf(emptyList())
                else combine(
                    store.clubQuery(Collections.BOOKINGS, eventId, clubId).snapshots(),
                    store.clubQuery(Collections.ATTENDANCE, eventId, clubId).snapshots(),
                ) { bookings, records ->
                    val byUser = records?.documents.orEmpty().associateBy { it.getString("userId") }
                    bookings?.documents.orEmpty().filter { it.getString("status") == "booked" }.map { b ->
                        val uid = b.getString("userId").orEmpty()
                        val rec = byUser[uid]
                        AttendeeRow(
                            userId = uid, name = b.getString("name") ?: uid, studentId = b.getString("studentId").orEmpty(),
                            arrival = if (rec?.get("arrival") != null) StageStatus.VERIFIED else StageStatus.NOT_SCANNED,
                            checkpoint = if (rec?.get("checkpoint") != null) StageStatus.VERIFIED else StageStatus.NOT_SCANNED,
                            flagged = rec?.getString("flagged"),
                        )
                    }.sortedBy { it.name }
                }
            }
}
