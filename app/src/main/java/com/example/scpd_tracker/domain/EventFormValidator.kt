package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.AttendanceMode
import java.time.ZonedDateTime

/** What the committee has typed into Create / Edit event. Capacity stays a String so invalid input can be shown. */
data class EventForm(
    val title: String = "",
    val description: String = "",
    val venue: String = "",
    val start: ZonedDateTime? = null,
    val end: ZonedDateTime? = null,
    val capacity: String = "",
    val tags: List<String> = emptyList(),
    val attendanceMode: AttendanceMode = AttendanceMode.ArrivalOnly,
    val pointsRequested: Boolean = true,
    val pointsJustification: String = "",
)

enum class FormError(val message: String) {
    TitleLength("Use 3 to 80 characters."),
    DescriptionTooShort("Describe the event in at least 20 characters."),
    VenueMissing("Add a venue."),
    StartMissing("Choose a start date and time."),
    EndBeforeStart("The end must be after the start."),
    TooSoon("Submit at least 2 days before the start, so the s-CPD office can review it."),
    CapacityInvalid("Enter a whole number from 1 to 500."),
    TagsCount("Choose 1 to 3 interest tags."),
    JustificationTooShort("Explain in at least 20 characters why this event earns s-CPD points."),
}

/** Validates the event form before it is saved or submitted for review (R1). Keys are field names. */
object EventFormValidator {
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val VENUE = "venue"
    const val START = "start"
    const val END = "end"
    const val CAPACITY = "capacity"
    const val TAGS = "tags"
    const val JUSTIFICATION = "justification"

    fun validate(form: EventForm, now: ZonedDateTime): Map<String, FormError> = buildMap {
        if (form.title.trim().length !in 3..80) put(TITLE, FormError.TitleLength)
        if (form.description.trim().length < 20) put(DESCRIPTION, FormError.DescriptionTooShort)
        if (form.venue.isBlank()) put(VENUE, FormError.VenueMissing)
        when {
            form.start == null -> put(START, FormError.StartMissing)
            form.start.isBefore(now.plus(Policy.MIN_SUBMIT_LEAD)) -> put(START, FormError.TooSoon)
        }
        if (form.start != null && form.end != null && !form.end.isAfter(form.start)) put(END, FormError.EndBeforeStart)
        if (form.start != null && form.end == null) put(END, FormError.EndBeforeStart)
        val capacity = form.capacity.trim().toIntOrNull()
        if (capacity == null || capacity !in 1..Policy.MAX_CAPACITY) put(CAPACITY, FormError.CapacityInvalid)
        if (form.tags.size !in 1..3) put(TAGS, FormError.TagsCount)
        if (form.pointsRequested && form.pointsJustification.trim().length < 20) put(JUSTIFICATION, FormError.JustificationTooShort)
    }
}
