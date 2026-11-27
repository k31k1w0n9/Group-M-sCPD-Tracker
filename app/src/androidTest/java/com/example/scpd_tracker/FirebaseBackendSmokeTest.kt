package com.example.scpd_tracker

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The real app, talking to the Firebase emulators on the laptop, with the free-plan rules doing the checks.
 * Skipped in the normal (fake backend) build. To run it:
 *   cd firebase && npm run emulators          (keep it running), then  npm run seed
 *   ./gradlew connectedDebugAndroidTest -Pscpd.backend=firebase \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.example.scpd_tracker.FirebaseBackendSmokeTest
 * Re-run the seed before each run: the test changes the data.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseBackendSmokeTest {
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ScpdApplication).container
    private val live = "leadership-workshop"

    @Test
    fun studentAndCommitteeFlowsRunOnFirebase() = runBlocking {
        assumeTrue("Run with -Pscpd.backend=firebase", container.usesFirebase)
        // Newer Android blocks the network for an app with no screen showing, so bring the app to the front first.
        val screen = ActivityScenario.launch(MainActivity::class.java)
        withTimeout(120_000) {
            container.auth.signOut()

            // Committee: open the checkpoint and read both live codes (made on the phone from the session secret).
            container.auth.signIn("committee.tech@scpd.example", "scpd-demo", AccountRole.Committee).let { r -> assertTrue("sign-in committee.tech@scpd.example: $r", r is SignInResult.Success) }
            assertTrue(container.attendance.openCheckpoint(live).isSuccess)
            val arrival = assertNotNullAndGet(container.attendance.currentCode(live, Stage.Arrival))
            val checkpoint = assertNotNullAndGet(container.attendance.currentCode(live, Stage.Checkpoint))
            val attendees = container.attendance.liveAttendees(live).first { it.isNotEmpty() }
            assertEquals(22, attendees.size)
            container.auth.signOut()

            // A committee account can't sign in as a student role it doesn't have, and a wrong password fails.
            container.auth.signIn("student1@scpd.example", "wrong", AccountRole.Student).let { r -> assertTrue("sign-in student1@scpd.example: $r", r is SignInResult.InvalidCredentials) }

            // Student: sees approved events, books and cancels a place.
            container.auth.signIn("student1@scpd.example", "scpd-demo", AccountRole.Student).let { r -> assertTrue("sign-in student1@scpd.example: $r", r is SignInResult.Success) }
            val events = container.events.studentEvents().first { it.isNotEmpty() }
            assertTrue(events.any { it.id == "build-with-ai" })
            assertTrue(events.none { it.id == "ai-ethics" }) // drafts stay hidden
            val booked = container.bookings.book("open-mic", UUID.randomUUID().toString())
            assertTrue("open-mic: $booked", booked is BookResult.Booked)
            assertTrue(container.bookings.myBookings().first { list -> list.any { it.eventId == "open-mic" } }.isNotEmpty())
            assertTrue(container.bookings.cancel((booked as BookResult.Booked).booking.id).isSuccess)

            // Student: live QR arrival is accepted by the rules.
            val device = container.deviceId.get()
            fun claim(stage: Stage, token: String) = AttendanceClaim(UUID.randomUUID().toString(), live, stage, token, device, container.clock.now(), false)
            val arrived = container.attendance.submitClaim(claim(Stage.Arrival, arrival.qrPayload))
            assertEquals(ClaimResult.Verified, arrived)

            // Student offline: the checkpoint scan is saved in Room, then uploads when the phone is back online.
            container.network.setForcedOffline(true)
            container.network.isOnline.first { !it }
            assertEquals(ClaimResult.PendingSync, container.attendance.submitClaim(claim(Stage.Checkpoint, checkpoint.qrPayload)))
            assertEquals(1, container.attendance.pendingCount().first())
            container.network.setForcedOffline(false)
            container.network.isOnline.first { it }
            container.attendance.syncPending()
            assertEquals(0, container.attendance.pendingCount().first())
            val record = container.attendance.myAttendance().first { list ->
                list.any { it.eventId == live && it.checkpoint == StageStatus.VERIFIED }
            }.first { it.eventId == live }
            assertEquals(StageStatus.VERIFIED, record.arrival)

            // Points: the +3 was written once for the complete record.
            val ledger = container.progress.ledger().first { list -> list.any { it.eventId == live } }
            assertEquals(3, ledger.filter { it.eventId == live }.sumOf { it.points })
            container.auth.signOut()

            // A second account on the same phone is saved as Under review, never Verified.
            container.auth.signIn("student2@scpd.example", "scpd-demo", AccountRole.Student).let { r -> assertTrue("sign-in student2@scpd.example: $r", r is SignInResult.Success) }
            val second = container.attendance.submitClaim(claim(Stage.Arrival, arrival.qrPayload).copy(requestId = UUID.randomUUID().toString()))
            assertTrue("second account: $second", second is ClaimResult.Rejected && second.reason.startsWith("Flagged for review"))
            container.auth.signOut()
        }
        screen.close()
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
