package com.example.scpd_tracker

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Evidence for the lecturer's "permission switched off in Settings while the app is closed" case. Revoking a
 * permission makes Android kill the app's process. The student must come back signed in, with the scan saved
 * offline still on the phone, and it must still upload.
 *
 * Two phases with the revoke in between, so it runs from firebase/scripts/revoke-evidence.sh, not from Gradle:
 *   phase1: sign in, go offline, scan (saved in Room)
 *   (script) adb shell pm revoke com.example.scpd_tracker android.permission.CAMERA   -> process killed
 *   phase2: new process: still signed in, scan still queued, camera denied; back online it uploads and is Verified
 */
@RunWith(AndroidJUnit4::class)
class PermissionRevokedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = (context.applicationContext as ScpdApplication).container
    private val live = "leadership-workshop"

    @Test
    fun phase1_scanSavedOffline() = runBlocking {
        assumeTrue("Run with -Pscpd.backend=firebase", container.usesFirebase)
        val screen = ActivityScenario.launch(MainActivity::class.java) // the network needs the app in the foreground
        withTimeout(60_000) {
            container.auth.signOut()
            container.auth.signIn("committee.tech@scpd.example", "scpd-demo", AccountRole.Committee).let { assertTrue("$it", it is SignInResult.Success) }
            val code = checkNotNull(container.attendance.currentCode(live, Stage.Arrival))
            container.auth.signOut()
            container.auth.signIn("student1@scpd.example", "scpd-demo", AccountRole.Student).let { assertTrue("$it", it is SignInResult.Success) }
            container.network.setForcedOffline(true)
            container.network.isOnline.first { !it }
            val claim = AttendanceClaim(UUID.randomUUID().toString(), live, Stage.Arrival, code.qrPayload, container.deviceId.get(), container.clock.now(), false)
            assertEquals(ClaimResult.PendingSync, container.attendance.submitClaim(claim))
            assertEquals(1, container.attendance.pendingCount().first())
        }
        screen.close()
    }

    @Test
    fun phase2_afterRevoke_signedInWithQueueIntact() = runBlocking {
        assumeTrue("Run with -Pscpd.backend=firebase", container.usesFirebase)
        // The camera was switched off while the app was closed.
        assertEquals(PackageManager.PERMISSION_DENIED, ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA))
        withTimeout(60_000) {
            // A brand-new process: the session comes back from disk (Firebase Auth + the saved role in DataStore).
            val user = container.auth.currentUser.filterNotNull().first()
            assertEquals("student1@scpd.example", user.email)
            assertEquals(AccountRole.Student, user.role)
            // The scan saved offline survived in Room.
            assertEquals(1, container.attendance.pendingCount().first())
            val screen = ActivityScenario.launch(MainActivity::class.java)
            container.attendance.syncPending()
            assertEquals(0, container.attendance.pendingCount().first())
            val record = container.attendance.myAttendance().first { list -> list.any { it.eventId == live && it.arrival == StageStatus.VERIFIED } }
            assertTrue(record.isNotEmpty())
            screen.close()
        }
    }
}
