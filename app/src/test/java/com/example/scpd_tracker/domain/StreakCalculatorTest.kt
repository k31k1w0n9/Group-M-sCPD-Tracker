package com.example.scpd_tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    // Mondays of four consecutive weeks in Nov 2026.
    private val w1 = LocalDate.parse("2026-11-02")
    private val w2 = LocalDate.parse("2026-11-09")
    private val w3 = LocalDate.parse("2026-11-16")
    private val w4 = LocalDate.parse("2026-11-23")
    private val everyWeekHasEvents = setOf(w1, w2, w3, w4)

    @Test
    fun threeConsecutiveWeeks_is3() {
        val verified = listOf(w2.plusDays(2), w3.plusDays(1), w4.plusDays(3))
        assertEquals(3, StreakCalculator.currentStreak(verified, everyWeekHasEvents, w4.plusDays(4)))
    }

    @Test
    fun gapWeekWithApprovedEvents_breaksStreak() {
        val verified = listOf(w1.plusDays(1), w3.plusDays(1), w4.plusDays(1))
        assertEquals(2, StreakCalculator.currentStreak(verified, everyWeekHasEvents, w4.plusDays(2)))
    }

    @Test
    fun gapWeekWithNoApprovedEvents_isSkipped() {
        // Week 2 had no approved events at all (e.g. exam week), so missing it does not break the streak.
        val verified = listOf(w1.plusDays(1), w3.plusDays(1), w4.plusDays(1))
        assertEquals(3, StreakCalculator.currentStreak(verified, setOf(w1, w3, w4), w4.plusDays(2)))
    }

    @Test
    fun currentWeekEmpty_countsFromLastWeek() {
        val verified = listOf(w2.plusDays(1), w3.plusDays(1))
        assertEquals(2, StreakCalculator.currentStreak(verified, everyWeekHasEvents, w4.plusDays(1)))
    }

    @Test
    fun noVerifiedAttendance_isZero() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList(), everyWeekHasEvents, w4))
    }

    @Test
    fun sundayNightAndMondayMorning_areDifferentWeeks() {
        val sunday = LocalDate.parse("2026-11-22") // Sun 23:59 MYT, still week 3
        val monday = LocalDate.parse("2026-11-23") // Mon 00:00 MYT, week 4
        assertEquals(w3, StreakCalculator.weekStart(sunday))
        assertEquals(w4, StreakCalculator.weekStart(monday))
        assertEquals(2, StreakCalculator.currentStreak(listOf(sunday, monday), everyWeekHasEvents, monday))
    }
}
