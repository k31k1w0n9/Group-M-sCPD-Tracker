package com.example.scpd_tracker.data.system

import java.time.DayOfWeek
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

        val morning = intended.toLocalDate().atTime(8, 0).atZone(zone)
        // if morning is too late, send before quiet hours instead.
        return if (morning.isBefore(start)) morning
        else intended.toLocalDate().minusDays(1).atTime(21, 59).atZone(zone)
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
