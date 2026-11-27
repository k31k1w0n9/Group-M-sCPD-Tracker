package com.example.scpd_tracker.ui

import android.app.UiAutomation
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
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

/** How long one step may take. Slow emulators (software rendering, the markers' laptops) need more than 5 s. */
private const val STEP_MS = 15_000L

/** Whole-app journeys through the real Activity, navigation and fakes. */
class EndToEndTest {
    /** Granted before the Activity starts, so no system dialog covers the screen mid-journey. */
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        *(listOf(Manifest.permission.CAMERA) +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList())
            .toTypedArray(),
    )
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    /** The in-memory backend lives as long as the app process, so each test starts signed out. */
    @Before
    fun signedOut() {
        val container = (rule.activity.application as ScpdApplication).container
        runBlocking { container.auth.signOut() }
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Continue to sign in")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun signIn(email: String, committee: Boolean = false) {
        // Each node is scrolled into view first: on a Pixel 2-height screen the form runs below the fold.
        rule.onNodeWithText(if (committee) "Club Committee" else "Student").performScrollTo().performClick()
        rule.onNodeWithText("Continue to sign in").performScrollTo().performClick()
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Sign in to s-CPD")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("University email").performScrollTo().performTextInput(email)
        rule.onNodeWithContentDescription("Password").performScrollTo().performTextInput(FakeData.PASSWORD)
        rule.onNodeWithText("Sign in").performScrollTo().performClick()
    }

    @Test
    fun studentSignsIn_seesHomeWithHappeningNow() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Happening now").assertIsDisplayed()
    }

    @Test
    fun studentProfile_opensLiveCheckInAndSavedNotificationPreferences() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        // The bottom bar is hidden while the keyboard is open; on a slow device it takes a moment to close.
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Profile") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText("Profile") and hasClickAction()).performClick()
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
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("My Events")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Explore").assertIsDisplayed()
    }

    @Test
    fun committeeSignsIn_seesOwnClubDashboard() {
        signIn(FakeData.COMMITTEE_TECH, committee = true)
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Manage your upcoming and past events.")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("University of Nottingham").assertIsDisplayed()
        rule.onNodeWithText("Computer Science Society", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Create event").assertIsDisplayed()
        for (tab in listOf("Check-in", "Account")) {
            rule.onNode(hasText(tab) and hasClickAction()).performClick()
            rule.onNodeWithContentDescription("University of Nottingham").assertIsDisplayed()
        }
    }

    @Test
    fun studentTabs_startWithBrandedCampusHeaders() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        for (tab in listOf("Home", "Explore", "My Events", "Progress", "Profile")) {
            rule.onNode(hasText(tab) and hasClickAction()).performClick()
            rule.onNodeWithContentDescription("University of Nottingham").assertIsDisplayed()
        }
    }

    @Test
    fun rotation_keepsTheSignedInScreen() {
        signIn(FakeData.STUDENT_1)
        rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        // Turn the device itself, like a student turning the phone. Asking the Activity for landscape
        // (requestedOrientation) is ignored by the Pixel 2 / API 31 emulator, and the test then waited forever.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
            rule.waitUntil(STEP_MS) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Home", substring = false)).fetchSemanticsNodes().isNotEmpty() }
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
            rule.waitUntil(STEP_MS) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
            rule.waitUntil(STEP_MS) { rule.onAllNodes(hasText("Hello, Aisha!")).fetchSemanticsNodes().isNotEmpty() }
        } finally {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
            automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }
}
