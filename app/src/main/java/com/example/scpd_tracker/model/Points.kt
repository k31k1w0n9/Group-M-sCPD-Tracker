package com.example.scpd_tracker.model

import java.time.LocalDate

/** One line in the points ledger: +3 for verified attendance, or -3 when the office reverses an award. */
data class PointsEntry(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val date: LocalDate,
    val points: Int,
    val reason: String,
)
