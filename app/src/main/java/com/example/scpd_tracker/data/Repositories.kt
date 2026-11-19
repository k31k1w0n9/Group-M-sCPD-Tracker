package com.example.scpd_tracker.data

import com.example.scpd_tracker.domain.EventForm
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.AttendeeRow
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.CheckInCode
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.PointsEntry
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/*
 * The data layer contracts. ViewModels only ever see these interfaces. This week the app runs on the
 * in-memory fakes in data/fake; next week Firebase implementations replace them without touching any screen.
 */

interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>
    suspend fun signIn(email: String, password: String, requestedRole: AccountRole): SignInResult
    suspend fun signOut()
    suspend fun saveInterests(interests: List<String>)
}

interface EventRepository {
    /** Approved events a student can see, including cancelled ones so My Events can explain them. */
    fun studentEvents(): Flow<List<Event>>
    fun event(id: String): Flow<Event?>
    /** Every event of one club, in any review status. */
    fun clubEvents(clubId: String): Flow<List<Event>>
    suspend fun saveDraft(clubId: String, form: EventForm, eventId: String?): Result<String>
    suspend fun submitForReview(eventId: String): Result<Unit>
    suspend fun withdraw(eventId: String): Result<Unit>
    suspend fun cancelEvent(eventId: String, reason: String): Result<Unit>
}

interface BookingRepository {
    fun myBookings(): Flow<List<Booking>>
    fun myNoShowCount(): Flow<Int>
    /** [requestId] makes a retry or a double tap safe: the same id never books twice. */
    suspend fun book(eventId: String, requestId: String): BookResult
    suspend fun joinWaitlist(eventId: String, requestId: String): BookResult
    suspend fun cancel(bookingId: String): Result<Unit>
    fun eventBookings(eventId: String): Flow<List<Pair<Booking, UserProfile>>>
}

interface AttendanceRepository {
    /** The signed-in student's records, with scans still on the phone shown as Saved on phone. */
    fun myAttendance(): Flow<List<AttendanceRecord>>
    fun pendingCount(): Flow<Int>
    fun lastSyncedAt(): Flow<Instant?>
    suspend fun submitClaim(claim: AttendanceClaim): ClaimResult
    /** Uploads saved scans. Returns how many were sent. Runs by itself when the connection comes back. */
    suspend fun syncPending(): Int

    // Committee side
    suspend fun openCheckIn(eventId: String): Result<Unit>
    suspend fun openCheckpoint(eventId: String): Result<Unit>
    suspend fun endCheckIn(eventId: String): Result<Unit>
    /** The live code, or null when it cannot be refreshed (offline, or the session is closed). */
    suspend fun currentCode(eventId: String, stage: Stage): CheckInCode?
    fun liveAttendees(eventId: String): Flow<List<AttendeeRow>>
}

interface ProgressRepository {
    fun ledger(): Flow<List<PointsEntry>>
}

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

/** The time source. Tests and the debug menu move it forward; nothing else reads the system clock. */
interface AppClock {
    fun now(): Instant
}

/** A stable random ID for this app install, sent with every check-in so one phone can't check in several accounts. */
interface DeviceIdSource {
    suspend fun get(): String
}
