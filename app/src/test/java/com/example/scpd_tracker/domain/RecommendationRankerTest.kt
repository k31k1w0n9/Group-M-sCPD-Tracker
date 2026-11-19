package com.example.scpd_tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationRankerTest {
    private val now = myt("2026-11-20T10:00")

    @Test
    fun interestMatch_ranksFirst() {
        val sports = testEvent(id = "sports", tags = listOf("Sports"), start = "2026-11-21T10:00", end = "2026-11-21T11:00")
        val tech = testEvent(id = "tech", tags = listOf("Tech"), start = "2026-11-28T10:00", end = "2026-11-28T11:00")
        val ranked = RecommendationRanker.rank(listOf(sports, tech), listOf("Tech"), emptyList(), emptyList(), now)
        assertEquals("tech", ranked.first().event.id)
    }

    @Test
    fun clashingEvent_excluded() {
        val booked = testEvent(id = "booked", start = "2026-11-25T14:00", end = "2026-11-25T16:00")
        val clash = testEvent(id = "clash", start = "2026-11-25T15:00", end = "2026-11-25T17:00")
        val ranked = RecommendationRanker.rank(listOf(clash), listOf("Tech"), emptyList(), listOf(booked), now)
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun alreadyBookedEvent_excluded() {
        val booked = testEvent(id = "booked")
        val ranked = RecommendationRanker.rank(listOf(booked), listOf("Tech"), emptyList(), listOf(booked), now)
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun fullEvent_rankedBelowOpen() {
        val full = testEvent(id = "full", booked = 40, start = "2026-11-24T10:00", end = "2026-11-24T11:00")
        val open = testEvent(id = "open", booked = 10, start = "2026-11-24T12:00", end = "2026-11-24T13:00")
        val ranked = RecommendationRanker.rank(listOf(full, open), listOf("Tech"), emptyList(), emptyList(), now)
        assertEquals(listOf("open", "full"), ranked.map { it.event.id })
    }

    @Test
    fun reasons_mentionMatchedInterest() {
        val ranked = RecommendationRanker.rank(listOf(testEvent()), listOf("Tech"), emptyList(), emptyList(), now)
        assertTrue(ranked.first().reasons.contains("Because you like Tech"))
    }

    @Test
    fun reasons_mentionNoClash() {
        val booked = testEvent(id = "booked", start = "2026-11-26T14:00", end = "2026-11-26T16:00")
        val ranked = RecommendationRanker.rank(listOf(testEvent()), listOf("Tech"), emptyList(), listOf(booked), now)
        assertTrue(ranked.first().reasons.contains("Fits your schedule"))
    }

    @Test
    fun noInterests_fallsBackToSoonest() {
        val later = testEvent(id = "later", tags = listOf("Arts"), start = "2026-12-10T10:00", end = "2026-12-10T11:00")
        val sooner = testEvent(id = "sooner", tags = listOf("Arts"), start = "2026-12-08T10:00", end = "2026-12-08T11:00")
        val ranked = RecommendationRanker.rank(listOf(later, sooner), emptyList(), emptyList(), emptyList(), now)
        assertEquals("sooner", ranked.first().event.id)
    }

    @Test
    fun pastEvents_excluded() {
        val past = testEvent(start = "2026-11-19T10:00", end = "2026-11-19T11:00")
        assertTrue(RecommendationRanker.rank(listOf(past), listOf("Tech"), emptyList(), emptyList(), now).isEmpty())
    }
}
