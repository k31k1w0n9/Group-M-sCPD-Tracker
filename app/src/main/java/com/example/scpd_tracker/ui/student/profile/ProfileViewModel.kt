package com.example.scpd_tracker.ui.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.domain.activeCheckInStage
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val initials: String = "",
    val studentId: String = "",
    val interests: List<String> = emptyList(),
    val pendingCount: Int = 0,
    /** Set when the student tries to sign out with scans still on the phone. */
    val signOutWarning: Int? = null,
    /** True once signing out is confirmed; the screen then hands over to the shell. */
    val signOutConfirmed: Boolean = false,
    val checkIn: ProfileCheckIn? = null,
)

data class ProfileCheckIn(val event: Event, val stage: Stage, val arrival: StageStatus?)

class ProfileViewModel(
    private val auth: AuthRepository,
    private val attendance: AttendanceRepository,
    bookings: BookingRepository,
    events: EventRepository,
) : ViewModel() {
    private val local = MutableStateFlow(ProfileUiState())

    private val checkIn = combine(events.studentEvents(), bookings.myBookings(), attendance.myAttendance()) { allEvents, allBookings, records ->
        val booked = allBookings.filter { it.status == BookingStatus.Booked }.map { it.eventId }.toSet()
        val byEvent = records.associateBy { it.eventId }
        allEvents.sortedBy { it.start }.firstNotNullOfOrNull { event ->
            if (event.id !in booked) return@firstNotNullOfOrNull null
            val record = byEvent[event.id]
            if (record?.reviewReason != null) return@firstNotNullOfOrNull null
            event.activeCheckInStage(record)?.let { ProfileCheckIn(event, it, record?.arrival) }
        }
    }

    val state: StateFlow<ProfileUiState> = combine(auth.currentUser, attendance.pendingCount(), checkIn, local) { user, pending, activeCheckIn, l ->
        l.copy(
            name = user?.displayName.orEmpty(),
            initials = user?.displayName.orEmpty().split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
            studentId = user?.studentId.orEmpty(),
            interests = user?.interests.orEmpty(),
            pendingCount = pending,
            checkIn = if (user != null) activeCheckIn else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** Scans still on the phone would never reach the server after sign-out, so warn first. */
    fun requestSignOut() {
        viewModelScope.launch {
            val pending = attendance.pendingCount().first()
            local.update { if (pending > 0) it.copy(signOutWarning = pending) else it.copy(signOutConfirmed = true) }
        }
    }

    fun confirmSignOut() = local.update { it.copy(signOutWarning = null, signOutConfirmed = true) }
    fun dismissSignOut() = local.update { it.copy(signOutWarning = null) }

    fun saveInterests(interests: List<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            auth.saveInterests(interests)
            onSaved()
        }
    }
}
