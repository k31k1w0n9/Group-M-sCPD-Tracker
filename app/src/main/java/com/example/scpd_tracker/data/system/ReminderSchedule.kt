package com.example.scpd_tracker.data.system

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// keep reminder times out of the configured quiet period.
internal object ReminderSchedule {
    val zone: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

    fun canNotifyStudent(targetUid: String?, storedUid: String?): Boolean =
        targetUid != null && targetUid == storedUid

    fun inQuietHours(at: ZonedDateTime): Boolean = at.hour >= 22 || at.hour < 8

    fun reminderAt(start: ZonedDateTime, hoursBefore: Long, quietHours: Boolean): ZonedDateTime {
        val intended = start.minusHours(hoursBefore)
        if (!quietHours || !inQuietHours(intended)) return intended

        // The edges of the quiet period around the intended time: 21:59 just before it, 08:00 just after it.
        val afterMidnight = intended.hour < 8
        val evening = intended.toLocalDate().let { if (afterMidnight) it.minusDays(1) else it }.atTime(21, 59).atZone(zone)
        val morning = intended.toLocalDate().let { if (afterMidnight) it else it.plusDays(1) }.atTime(8, 0).atZone(zone)
        // Use whichever edge is closer to the intended time. The morning only works if it is still before the event.
        val morningGap = Duration.between(intended, morning)
        val eveningGap = Duration.between(evening, intended)
        return if (morning.isBefore(start) && morningGap < eveningGap) morning else evening
    }

    fun nextStreakAt(now: Instant): ZonedDateTime {
        val local = now.atZone(zone)
        var next = local.toLocalDate().atTime(18, 0).atZone(zone)
        while (next.dayOfWeek != DayOfWeek.SUNDAY || !next.isAfter(local)) {
            next = next.plusDays(1)
        }
        return next
    }
}
