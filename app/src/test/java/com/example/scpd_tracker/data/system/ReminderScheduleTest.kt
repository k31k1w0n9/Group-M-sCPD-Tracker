package com.example.scpd_tracker.data.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderScheduleTest {
    private fun local(value: String) = LocalDateTime.parse(value).atZone(ReminderSchedule.zone)

    @Test
    fun remindersAre24And1HoursBeforeAnEvent() {
        val start = local("2026-11-26T14:00")
        assertEquals(local("2026-11-25T14:00"), ReminderSchedule.reminderAt(start, 24, true))
        assertEquals(local("2026-11-26T13:00"), ReminderSchedule.reminderAt(start, 1, true))
    }

    @Test
    fun quietHoursRescheduleInsteadOfDropping() {
        val start = local("2026-11-26T08:00")
        val oneHour = ReminderSchedule.reminderAt(start, 1, true)
        assertEquals(local("2026-11-25T21:59"), oneHour)
        assertFalse(ReminderSchedule.inQuietHours(oneHour))
        assertTrue(oneHour.isBefore(start))

        val dayAhead = ReminderSchedule.reminderAt(local("2026-11-26T07:30"), 24, true)
        assertEquals(local("2026-11-25T08:00"), dayAhead)
    }

    @Test
    fun quietHoursDisabledKeepsTheRequestedTime() {
        val start = local("2026-11-26T08:30")
        assertEquals(local("2026-11-26T07:30"), ReminderSchedule.reminderAt(start, 1, false))
    }

    @Test
    fun reminderOwnerMustMatchTheBookedStudent() {
        assertTrue(ReminderSchedule.canNotifyStudent("u-aisha", "u-aisha"))
        assertFalse(ReminderSchedule.canNotifyStudent("u-aisha", "u-daniel"))
        assertFalse(ReminderSchedule.canNotifyStudent("u-aisha", null))
    }

    @Test
    fun streakRemindersRunOnSundayEvening() {
        val monday = local("2026-11-23T12:00").toInstant()
        assertEquals(local("2026-11-29T18:00"), ReminderSchedule.nextStreakAt(monday))
    }
}
