package com.example.scpd_tracker.domain

import java.time.Duration
import java.time.ZoneId

/** Every rule value the team may want to change lives here, in one place. */
object Policy {
    const val POINTS_PER_EVENT = 3
    const val MAX_ACTIVE_BOOKINGS = 5
    val CANCEL_CUTOFF: Duration = Duration.ofHours(2)
    /** No-shows in a semester before booking is paused. */
    const val NO_SHOW_PAUSE_THRESHOLD = 3
    val CODE_ROTATION: Duration = Duration.ofSeconds(30)
    /** Offline scans must reach the server within this time after check-in closes. */
    val OFFLINE_UPLOAD_WINDOW: Duration = Duration.ofHours(2)
    /** The s-CPD office needs time to review, so an event must start at least this long after submission. */
    val MIN_SUBMIT_LEAD: Duration = Duration.ofDays(2)
    const val MAX_CAPACITY = 500
    /** Wrong backup codes allowed per student per check-in session. */
    const val BACKUP_CODE_ATTEMPTS = 5
    /** Streak weeks and event dates use Malaysia time. */
    val ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

    /** Interest categories, in the same order as the Interests screen. */
    val INTEREST_CATEGORIES = listOf("Tech", "Career", "Leadership", "Sports", "Volunteering", "Arts")
}
