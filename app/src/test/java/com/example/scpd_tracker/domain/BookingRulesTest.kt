package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRulesTest {
    private val now = myt("2026-11-20T10:00")

    @Test
    fun full_returnsFullWithWaitlist() {
        val event = testEvent(capacity = 40, booked = 40)
        assertEquals(BookingDecision.Full(canWaitlist = true), BookingRules.canBook(event, emptyList(), 0, now))
    }

    @Test
    fun overlap_returnsClash() {
        val booked = testEvent(id = "a", start = "2026-11-25T14:00", end = "2026-11-25T16:00")
        val wanted = testEvent(id = "b", start = "2026-11-25T15:30", end = "2026-11-25T17:00")
        assertEquals(BookingDecision.Clash(booked), BookingRules.canBook(wanted, listOf(booked), 0, now))
    }

    @Test
    fun backToBack_isNotClash() {
        val booked = testEvent(id = "a", start = "2026-11-25T14:00", end = "2026-11-25T16:00")
        val wanted = testEvent(id = "b", start = "2026-11-25T16:00", end = "2026-11-25T17:00")
        assertEquals(BookingDecision.Allowed, BookingRules.canBook(wanted, listOf(booked), 0, now))
    }

    @Test
    fun eventAcrossMidnight_clashDetected() {
        val booked = testEvent(id = "a", start = "2026-11-25T23:00", end = "2026-11-26T01:00")
        val wanted = testEvent(id = "b", start = "2026-11-26T00:30", end = "2026-11-26T02:00")
        assertEquals(BookingDecision.Clash(booked), BookingRules.canBook(wanted, listOf(booked), 0, now))
    }

    @Test
    fun fiveActive_returnsCapReached() {
        val active = (1..Policy.MAX_ACTIVE_BOOKINGS).map {
            testEvent(id = "b$it", start = "2026-12-0${it}T10:00", end = "2026-12-0${it}T11:00")
        }
        assertEquals(BookingDecision.CapReached, BookingRules.canBook(testEvent(), active, 0, now))
    }

    @Test
    fun threeNoShows_returnsPaused() {
        assertEquals(
            BookingDecision.Paused,
            BookingRules.canBook(testEvent(), emptyList(), Policy.NO_SHOW_PAUSE_THRESHOLD, now),
        )
    }

    @Test
    fun cancelledEvent_returnsClosed() {
        val event = testEvent(state = EventState.Cancelled)
        assertEquals(BookingDecision.Closed, BookingRules.canBook(event, emptyList(), 0, now))
    }

    @Test
    fun notApprovedEvent_returnsClosed() {
        val event = testEvent(review = ReviewStatus.PendingReview)
        assertEquals(BookingDecision.Closed, BookingRules.canBook(event, emptyList(), 0, now))
    }

    @Test
    fun startedEvent_returnsClosed() {
        val event = testEvent(start = "2026-11-20T09:00", end = "2026-11-20T12:00")
        assertEquals(BookingDecision.Closed, BookingRules.canBook(event, emptyList(), 0, now))
    }

    @Test
    fun alreadyBooked_returnsAlreadyBooked() {
        val event = testEvent()
        assertEquals(BookingDecision.AlreadyBooked, BookingRules.canBook(event, listOf(event), 0, now))
    }

    @Test
    fun canCancel_exactlyAtCutoff_false() {
        val event = testEvent(start = "2026-11-25T14:00")
        assertFalse(BookingRules.canCancel(event, myt("2026-11-25T12:00")))
    }

    @Test
    fun canCancel_oneMinuteBeforeCutoff_true() {
        val event = testEvent(start = "2026-11-25T14:00")
        assertTrue(BookingRules.canCancel(event, myt("2026-11-25T11:59")))
    }
}
