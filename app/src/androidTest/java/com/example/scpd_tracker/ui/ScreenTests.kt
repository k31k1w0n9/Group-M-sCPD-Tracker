package com.example.scpd_tracker.ui

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.auth.SignIn
import com.example.scpd_tracker.ui.student.checkin.CheckInScreen
import com.example.scpd_tracker.ui.student.checkin.CheckInUiState
import com.example.scpd_tracker.ui.student.checkin.StageUi
import com.example.scpd_tracker.ui.student.details.EventDetailsScreen
import com.example.scpd_tracker.ui.student.details.EventDetailsUiState
import com.example.scpd_tracker.ui.student.home.EventCardUi
import com.example.scpd_tracker.ui.student.home.HomeScreen
import com.example.scpd_tracker.ui.student.home.HomeUiState
import com.example.scpd_tracker.ui.student.profile.ProfileScreen
import com.example.scpd_tracker.ui.student.profile.ProfileUiState
import com.example.scpd_tracker.ui.student.progress.ProgressUiState
import com.example.scpd_tracker.ui.theme.ScpdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

/** Compose UI tests on the stateless screens: each is fed a UiState and checked for what the student sees. */
class ScreenTests {
    @get:Rule val rule = createComposeRule()

    private val pixel2 = DpSize(411.dp, 731.dp)

    private fun event(id: String, title: String = "Event $id", capacity: Int = 40, booked: Int = 10, mode: AttendanceMode = AttendanceMode.ArrivalOnly) = Event(
        id = id, clubId = "cs-society", clubName = "Computer Science Society", title = title,
        description = "A description of the event for the test.", start = ZonedDateTime.now(Policy.ZONE).plusDays(3),
        end = ZonedDateTime.now(Policy.ZONE).plusDays(3).plusHours(2), venue = "F1A02", capacity = capacity, bookedCount = booked,
        tags = listOf("Tech"), reviewStatus = ReviewStatus.Approved, attendanceMode = mode, organiserContact = "cs@club.example",
    )

    private fun details(state: EventDetailsUiState) = rule.setContent {
        ScpdTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(pixel2)) {
                EventDetailsScreen(state, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun fullEvent_offersJoinWaitlist() {
        val full = event("full", capacity = 25, booked = 25)
        details(EventDetailsUiState(loading = false, event = full, decision = BookingDecision.Full(true)))
        rule.onNodeWithText("Join waitlist").assertIsDisplayed()
    }

    @Test
    fun clash_disablesBookingAndSaysWhy() {
        val other = event("other", title = "Python: Next Steps")
        details(EventDetailsUiState(loading = false, event = event("e"), decision = BookingDecision.Clash(other)))
        rule.onNodeWithText("Can't book").assertIsNotEnabled()
        rule.onNodeWithText("Clashes with Python: Next Steps", substring = true).assertIsDisplayed()
    }

    @Test
    fun longTitle_bookButtonStillVisibleOnPixel2() {
        val long = event("long", title = "A very long workshop title about building accessible mobile apps for everyone")
        details(EventDetailsUiState(loading = false, event = long, decision = BookingDecision.Allowed))
        rule.onNodeWithText("Book event").assertIsDisplayed()
    }

    @Test
    fun checkpointEvent_showsTwoScanRequirement() {
        details(EventDetailsUiState(loading = false, event = event("cp", mode = AttendanceMode.ArrivalAndCheckpoint), decision = BookingDecision.Allowed))
        rule.onNodeWithText("Needs 2 scans").assertExists()
    }

    @Test
    fun homeOnPixel2_firstEventCardVisibleWithoutScroll() {
        val cards = (1..3).map { EventCardUi(event("e$it"), "Because you like Tech", booked = false) }
        rule.setContent {
            ScpdTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(pixel2)) {
                    HomeScreen(HomeUiState(greetingName = "Aisha", cards = cards, isLoading = false), {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        rule.onNodeWithText("Hello, Aisha!").assertIsDisplayed()
        rule.onAllNodesWithTag("event_card").onFirst().assertIsDisplayed()
    }

    @Test
    fun homeAfterShortScroll_onlyOneSearchFieldIsExposed() {
        val cards = (1..12).map { EventCardUi(event("e$it"), "Because you like Tech", booked = false) }
        rule.setContent {
            ScpdTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(pixel2)) {
                    HomeScreen(HomeUiState(greetingName = "Aisha", cards = cards), {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        rule.onNodeWithTag("home_list").performTouchInput {
            swipe(
                start = Offset(center.x, height * 0.66f),
                end = Offset(center.x, height * 0.52f),
                durationMillis = 750,
            )
        }
        rule.onAllNodesWithContentDescription("Search events, clubs or skills").assertCountEquals(1)
        rule.onNodeWithTag("home_list").performTouchInput {
            swipe(Offset(center.x, height * 0.85f), Offset(center.x, height * 0.15f), durationMillis = 550)
        }
        rule.onNodeWithText("Home").assertIsDisplayed()
        rule.onNodeWithContentDescription("University of Nottingham").assertIsNotDisplayed()
        rule.onAllNodesWithContentDescription("Search events, clubs or skills").assertCountEquals(1)

        rule.onNodeWithTag("home_list").performTouchInput {
            swipe(Offset(center.x, height * 0.1f), Offset(center.x, height * 0.95f), durationMillis = 550)
        }
        rule.onNodeWithText("Hello, Aisha!").assertIsDisplayed()
        rule.onNodeWithContentDescription("University of Nottingham").assertIsDisplayed()
    }

    @Test
    fun profileAvatar_straddlesTopEdgeOfIdentityCard() {
        rule.setContent {
            ScpdTheme {
                ProfileScreen(ProfileUiState(name = "Aisha Rahman", initials = "AR"), ProgressUiState(), {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        val avatar = rule.onNodeWithTag("profile_avatar").fetchSemanticsNode().boundsInRoot
        val card = rule.onNodeWithTag("profile_identity_card").fetchSemanticsNode().boundsInRoot
        assertEquals("Card must start halfway down the avatar", avatar.center.y, card.top, 1f)
    }

    @Test
    fun checkInOffline_backupCodeExplainsItNeedsInternet() {
        val state = CheckInUiState(
            event = event("live"), activeStage = Stage.Arrival, isOffline = true,
            stages = listOf(StageUi(Stage.Arrival, StageStatus.NOT_SCANNED, true, "Scan the QR on the organiser's screen.")),
        )
        rule.setContent { ScpdTheme { CheckInScreen(state, false, {}, {}, {}, {}, {}, {}, null) } }
        rule.onNodeWithText("Backup codes need internet. Scan the QR instead, or reconnect.").assertExists()
        rule.onNodeWithText("Scan arrival QR").assertIsDisplayed()
    }

    @Test
    fun savedOfflineScan_showsSavedOnPhoneChip() {
        val state = CheckInUiState(
            event = event("live"), isOffline = true, pendingUploads = 1,
            stages = listOf(StageUi(Stage.Arrival, StageStatus.PENDING_SYNC, true, "")),
        )
        rule.setContent { ScpdTheme { CheckInScreen(state, false, {}, {}, {}, {}, {}, {}, null) } }
        rule.onNodeWithText("Saved on phone").assertIsDisplayed()
        rule.onNodeWithText("1 scan waiting to upload").assertExists()
    }

    @Test
    fun signInError_showsBannerAndKeepsButton() {
        rule.setContent {
            ScpdTheme {
                SignIn(AccountRole.Student, "a@b.c", "", SignInResult.InvalidCredentials, false, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Email or password not recognised").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sign in").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signingIn_disablesButton() {
        rule.setContent {
            ScpdTheme { SignIn(AccountRole.Student, "a@b.c", "x", null, true, {}, {}, {}, {}, {}, {}, {}) }
        }
        rule.onNodeWithText("Signing in…").assertIsNotEnabled()
    }

    @Test
    fun outcomeTag_presentOnCheckIn() {
        rule.setContent { ScpdTheme { CheckInScreen(CheckInUiState(event = event("x")), false, {}, {}, {}, {}, {}, {}, null) } }
        rule.onNodeWithTag("outcome").assertExists()
    }
}
