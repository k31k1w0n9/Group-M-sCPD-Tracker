package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.ReviewStatus
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** Builders shared by the domain tests. Times are Malaysia time, the zone the app uses. */
internal fun myt(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(Policy.ZONE)

internal fun testEvent(
    id: String = "e1",
    start: String = "2026-11-25T14:00",
    end: String = "2026-11-25T16:00",
    capacity: Int = 40,
    booked: Int = 10,
    tags: List<String> = listOf("Tech"),
    review: ReviewStatus = ReviewStatus.Approved,
    state: EventState = EventState.BookingOpen,
    title: String = "Event $id",
) = Event(
    id = id, clubId = "cs-society", clubName = "Computer Science Society", title = title,
    description = "A description long enough for the form rules.", start = myt(start), end = myt(end),
    venue = "F1A02", capacity = capacity, bookedCount = booked, tags = tags, reviewStatus = review,
    state = state, attendanceMode = AttendanceMode.ArrivalOnly, organiserContact = "cs@club.example",
)
