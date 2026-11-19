package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.AttendanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFormValidatorTest {
    private val now = myt("2026-11-20T10:00")
    private val valid = EventForm(
        title = "Build with AI",
        description = "Hands-on workshop on building small AI tools.",
        venue = "F1A02",
        start = myt("2026-11-25T14:00"),
        end = myt("2026-11-25T16:00"),
        capacity = "40",
        tags = listOf("Tech"),
        attendanceMode = AttendanceMode.ArrivalOnly,
        pointsRequested = true,
        pointsJustification = "Skills workshop open to every student.",
    )

    private fun errorsFor(form: EventForm) = EventFormValidator.validate(form, now)

    @Test fun validForm_noErrors() = assertTrue(errorsFor(valid).isEmpty())

    @Test fun titleTooShort() = assertEquals(FormError.TitleLength, errorsFor(valid.copy(title = "AI"))["title"])

    @Test fun titleTooLong() = assertEquals(FormError.TitleLength, errorsFor(valid.copy(title = "x".repeat(81)))["title"])

    @Test fun descriptionTooShort() =
        assertEquals(FormError.DescriptionTooShort, errorsFor(valid.copy(description = "Too short"))["description"])

    @Test fun venueMissing() = assertEquals(FormError.VenueMissing, errorsFor(valid.copy(venue = "  "))["venue"])

    @Test fun startMissing() = assertEquals(FormError.StartMissing, errorsFor(valid.copy(start = null))["start"])

    @Test fun endBeforeStart() =
        assertEquals(FormError.EndBeforeStart, errorsFor(valid.copy(end = myt("2026-11-25T13:00")))["end"])

    @Test fun tooSoon() = assertEquals(
        FormError.TooSoon,
        errorsFor(valid.copy(start = myt("2026-11-21T14:00"), end = myt("2026-11-21T16:00")))["start"],
    )

    @Test fun capacityNotANumber() = assertEquals(FormError.CapacityInvalid, errorsFor(valid.copy(capacity = "forty"))["capacity"])

    @Test fun capacityZero() = assertEquals(FormError.CapacityInvalid, errorsFor(valid.copy(capacity = "0"))["capacity"])

    @Test fun capacityAboveMax() = assertEquals(FormError.CapacityInvalid, errorsFor(valid.copy(capacity = "501"))["capacity"])

    @Test fun noTags() = assertEquals(FormError.TagsCount, errorsFor(valid.copy(tags = emptyList()))["tags"])

    @Test fun fourTags() =
        assertEquals(FormError.TagsCount, errorsFor(valid.copy(tags = listOf("Tech", "Arts", "Sports", "Career")))["tags"])

    @Test fun justificationTooShort() =
        assertEquals(FormError.JustificationTooShort, errorsFor(valid.copy(pointsJustification = "Fun"))["justification"])

    @Test fun noPointsRequested_justificationNotNeeded() =
        assertTrue(errorsFor(valid.copy(pointsRequested = false, pointsJustification = "")).isEmpty())
}
