package com.example.scpd_tracker.ui

import android.content.pm.ActivityInfo
import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.scpd_tracker.MainActivity
import com.example.scpd_tracker.ScpdApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import com.example.scpd_tracker.data.fake.FakeData
import org.junit.Rule
import org.junit.Test

/** Whole-app journeys through the real Activity, navigation and fakes. */
class EndToEndTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** The in-memory backend lives as long as the app process, so each test starts signed out. */
    @Before
    fun signedOut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                rule.activity.packageName, Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        val container = (rule.activity.application as ScpdApplication).container
        runBlocking { container.auth.signOut() }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Continue to sign in")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun signIn(email: String, committee: Boolean = false) {
        // Each node is scrolled into view first: on a Pixel 2-height screen the form runs below the fold.
        rule.onNodeWithText(if (committee) "Club Committee" else "Student").performScrollTo().performClick()
        rule.onNodeWithText("Continue to sign in").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Sign in to s-CPD")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("University email").performScrollTo().performTextInput(email)
        rule.onNodeWithContentDescription("Password").performScrollTo().performTextInput(FakeData.PASSWORD)
        rule.onNodeWithText("Sign in").performScrollTo().performClick()
    }

    @Test
    fun studentSignsIn_seesHomeWithHappeningNow() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Happening now").assertIsDisplayed()
    }

    @Test
    fun studentProfile_opensLiveCheckInAndSavedNotificationPreferences() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Profile").performClick()
        rule.onNodeWithText("Weekly streak").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Scan arrival QR").performScrollTo().performClick()
        rule.onNodeWithText("Check-in").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Settings").performScrollTo().performClick()
        rule.onNodeWithText("Event reminders").assertDoesNotExist()
        rule.onNodeWithText("Notifications").performScrollTo().performClick()
        rule.onNodeWithText("Quiet hours").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun committeeAccount_canUseStudentApp() {
        signIn(FakeData.COMMITTEE_TECH)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("My Events")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Explore").assertIsDisplayed()
    }

    @Test
    fun committeeSignsIn_seesOwnClubDashboard() {
        signIn(FakeData.COMMITTEE_TECH, committee = true)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Events · Computer Science Society")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Create event").assertIsDisplayed()
    }

    @Test
    fun rotation_keepsTheSignedInScreen() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitForIdle()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Home", substring = false)).fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        rule.waitForIdle()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
    }
}
