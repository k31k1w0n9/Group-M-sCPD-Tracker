package com.example.scpd_tracker.ui.student.details

import com.example.scpd_tracker.data.fake.FakeData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EventDetailsPointsTest {
    private val event = FakeData.seed(Instant.parse("2026-11-23T12:00:00Z"))
        .events.first { it.id == "build-with-ai" }

    @Test
    fun ownClubMemberSeesZeroPoints() {
        assertEquals(
            "Own-club attendance earns 0 s-CPD points, even when verified.",
            eventPointsMessage(event, listOf("cs-society")),
        )
    }

    @Test
    fun otherClubMemberSeesThreePoints() {
        assertEquals(
            "Earn 3 s-CPD points once your attendance is verified.",
            eventPointsMessage(event, listOf("arts-club")),
        )
    }

    @Test
    fun eventWithoutPointsShowsNoAward() {
        assertEquals(
            "This event does not award s-CPD points.",
            eventPointsMessage(event.copy(pointsRequested = false), emptyList()),
        )
    }
