package com.example.scpd_tracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Weekly attendance streak (own feature 2). A week runs Monday to Sunday in Malaysia time.
 * A week with no approved events at all (exam or recess weeks) is skipped, so it never breaks a streak.
 */
object StreakCalculator {

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * @param verifiedEventDates dates of events with Verified attendance.
     * @param weeksWithApprovedEvents Mondays of weeks that had at least one approved event.
     */
    fun currentStreak(verifiedEventDates: List<LocalDate>, weeksWithApprovedEvents: Set<LocalDate>, today: LocalDate): Int {
        val verifiedWeeks = verifiedEventDates.map(::weekStart).toSet()
        val earliest = verifiedWeeks.minOrNull() ?: return 0
        var week = weekStart(today)
        // The current week is still running, so an empty one does not break the streak yet.
        if (week !in verifiedWeeks) week = week.minusWeeks(1)
        var streak = 0
        while (!week.isBefore(earliest)) {
            when {
                week in verifiedWeeks -> streak++
                week in weeksWithApprovedEvents -> return streak
            }
            week = week.minusWeeks(1)
        }
        return streak
    }
}
