package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.data.system.AdjustableClock
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AccountRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDateTime

/** A network the test can switch on and off. */
class TestNetwork(online: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = state
    fun set(online: Boolean) { state.value = online }
}

/** Fri 20 Nov 2026, 15:00 Malaysia time: the seed's live event started at 14:30. */
val TEST_NOW: Instant = LocalDateTime.parse("2026-11-20T15:00").atZone(Policy.ZONE).toInstant()

/** Wires the fakes together the same way AppContainer does, on a fixed clock. */
class TestApp(scope: CoroutineScope, online: Boolean = true) {
    val clock = AdjustableClock { TEST_NOW }
    val network = TestNetwork(online)
    val backend = FakeBackend(clock)
    val auth = FakeAuthRepository(backend, network)
    val events = FakeEventRepository(backend, auth, network)
    val bookings = FakeBookingRepository(backend, auth, network)
    val attendance = FakeAttendanceRepository(backend, auth, network, clock, scope)
    val progress = FakeProgressRepository(backend, auth)
    var deviceName = "phone-A"
    val deviceId = object : com.example.scpd_tracker.data.DeviceIdSource {
        override suspend fun get() = deviceName
    }

    suspend fun signInStudent1() = auth.signIn(FakeData.STUDENT_1, FakeData.PASSWORD, AccountRole.Student)
    suspend fun signInStudent2() = auth.signIn(FakeData.STUDENT_2, FakeData.PASSWORD, AccountRole.Student)
    suspend fun signInCommittee() = auth.signIn(FakeData.COMMITTEE_TECH, FakeData.PASSWORD, AccountRole.Committee)
}
