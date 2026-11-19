package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.ReviewStatus
import java.time.ZonedDateTime

data class Recommendation(val event: Event, val score: Int, val reasons: List<String>)

/**
 * Personalised recommendations (own feature 1). A small, explainable score instead of a plain tag filter:
 * every point of score comes with a reason the student can read on the card.
 */
object RecommendationRanker {
    private const val INTEREST_MATCH = 3
    private const val HISTORY_MATCH = 1
    private const val HISTORY_MAX = 2
    private const val SOON = 1
    private const val FULL_PENALTY = -2

    fun rank(
        events: List<Event>,
        interests: List<String>,
        attendedTags: List<String>,
        myBookedEvents: List<Event>,
        now: ZonedDateTime,
    ): List<Recommendation> = events
        .asSequence()
        .filter { it.reviewStatus == ReviewStatus.Approved && !it.isCancelled && it.start.isAfter(now) }
        .filter { candidate -> myBookedEvents.none { it.id == candidate.id } }
        // A clash means the student could not attend, so it is not recommended at all.
        .filter { candidate -> myBookedEvents.none { BookingRules.overlaps(it, candidate) } }
        .map { score(it, interests, attendedTags, myBookedEvents.isNotEmpty(), now) }
        .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.event.start })
        .toList()

    private fun score(event: Event, interests: List<String>, attendedTags: List<String>, hasBookings: Boolean, now: ZonedDateTime): Recommendation {
        var score = 0
        val reasons = mutableListOf<String>()
        event.tags.filter { it in interests }.forEach { tag ->
            score += INTEREST_MATCH
            reasons += "Because you like $tag"
        }
        val history = event.tags.filter { it in attendedTags }.take(HISTORY_MAX)
        if (history.isNotEmpty()) {
            score += HISTORY_MATCH * history.size
            reasons += "You enjoyed ${history.first()} events before"
        }
        if (hasBookings) reasons += "Fits your schedule"
        if (event.start.isBefore(now.plusDays(7))) {
            score += SOON
            reasons += "Happening this week"
        }
        if (event.isFull) {
            score += FULL_PENALTY
            reasons += "Full · join the waitlist"
        }
        if (reasons.isEmpty()) reasons += "Something new to try"
        return Recommendation(event, score, reasons)
    }
}
