package com.example.scpd_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.scpd_tracker.data.fake.FakeData
import com.example.scpd_tracker.ui.committee.DashboardScreen
import com.example.scpd_tracker.ui.committee.DashboardUiState
import com.example.scpd_tracker.ui.student.details.EventDetailsScreen
import com.example.scpd_tracker.ui.student.details.EventDetailsUiState
import com.example.scpd_tracker.ui.theme.ScpdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class M1ScreenFixesTest {
    @get:Rule val rule = createComposeRule()

    private val event = FakeData.seed(Instant.parse("2026-11-23T12:00:00Z"))
        .events.first { it.id == "build-with-ai" }

    @Test
    fun ownClubEventShowsZeroPoints() {
        rule.setContent {
            ScpdTheme {
                EventDetailsScreen(
                    state = EventDetailsUiState(loading = false, event = event),
                    onBack = {}, onBook = {}, onConfirm = {}, onDismissReview = {},
                    onJoinWaitlist = {}, onDismissResult = {}, onCheckIn = {}, onMyEvents = {},
                    memberClubIds = listOf("cs-society"),
                )
            }
        }
        rule.onNodeWithText("Own-club attendance earns 0 s-CPD points, even when verified.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun committeeCanOpenApprovedEventManagement() {
        var opened: String? = null
        rule.setContent {
            ScpdTheme {
                DashboardScreen(
                    state = DashboardUiState(clubName = "Computer Science Society", loading = false, upcoming = listOf(event)),
                    onCreate = {}, onOpen = { opened = it }, onBookings = {},
                )
            }
        }
        rule.onNodeWithTag("manage_event").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("build-with-ai", opened) }
    }
}
