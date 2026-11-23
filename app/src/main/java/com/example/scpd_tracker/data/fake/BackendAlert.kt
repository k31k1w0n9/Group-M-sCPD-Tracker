package com.example.scpd_tracker.data.fake

// the backend keeps each alert tied to its intended student.
enum class AlertKind(val heading: String) {
    Cancelled("Event cancelled"),
    Waitlist("Waitlist place confirmed"),
    Checkpoint("Checkpoint open"),
}

data class BackendAlert(
    val id: String,
    val recipientId: String,
    val eventId: String,
    val eventTitle: String,
    val startMillis: Long,
    val venue: String,
    val kind: AlertKind,
    val detail: String,
)
