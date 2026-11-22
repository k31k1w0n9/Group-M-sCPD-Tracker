package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.R
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.StreakCalculator
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.PointsEntry
import com.example.scpd_tracker.model.PosterArt
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class FakeSeed(
    val accounts: List<FakeBackend.Account>,
    val events: List<Event>,
    val bookings: List<Booking>,
    val evidence: Map<String, FakeBackend.Evidence>,
    val ledger: List<FakeBackend.LedgerRow>,
    val sessions: Map<String, FakeBackend.Session>,
)

/**
 * Demo data. Every date is relative to "today", so the demo always has an event happening now,
 * upcoming events, a full one, a clashing pair and three weeks of history, whenever it is marked.
 */
object FakeData {
    const val PASSWORD = "scpd-demo"
    const val STUDENT_1 = "student1@scpd.example"
    const val STUDENT_2 = "student2@scpd.example"
    const val COMMITTEE_TECH = "committee.tech@scpd.example"
    const val COMMITTEE_ARTS = "committee.arts@scpd.example"

    const val LIVE_EVENT = "leadership-workshop"

    val clubNames = mapOf(
        "cs-society" to "Computer Science Society",
        "arts-club" to "Arts Club",
        "rotaract" to "Rotaract Club of the University of Nottingham Malaysia",
        "sports" to "Sports Council",
    )
    val clubContacts = mapOf(
        "cs-society" to "cssociety@nottingham.edu.my",
        "arts-club" to "artsclub@nottingham.edu.my",
        "rotaract" to "rotaract@nottingham.edu.my",
        "sports" to "sports@nottingham.edu.my",
    )

    private val classmates = listOf(
        "Aiden Tan", "Bella Ong", "Chen Wei", "Divya Nair", "Ethan Lau", "Farah Aziz", "Grace Lim",
        "Hafiz Ismail", "Ivy Chong", "Jason Wong", "Kavya Rao", "Liam Ng", "Mei Ling", "Nadia Hassan",
        "Omar Rahim", "Priya Menon", "Qistina Zain", "Ryan Teo", "Sofia Yap", "Tariq Ali",
    )

    fun seed(now: Instant): FakeSeed {
        val zone = Policy.ZONE
        val today: LocalDate = now.atZone(zone).toLocalDate()
        val thisWeek = StreakCalculator.weekStart(today)
        fun at(days: Long, time: String): ZonedDateTime = today.plusDays(days).atTime(LocalTime.parse(time)).atZone(zone)
        fun weeksAgo(weeks: Long, dayOffset: Long, time: String): ZonedDateTime =
            thisWeek.minusWeeks(weeks).plusDays(dayOffset).atTime(LocalTime.parse(time)).atZone(zone)
        val liveStart = now.atZone(zone).minusMinutes(30).truncatedTo(ChronoUnit.MINUTES)

        fun event(
            id: String, club: String, title: String, start: ZonedDateTime, hours: Long, venue: String,
            capacity: Int, booked: Int, tags: List<String>, description: String,
            review: ReviewStatus = ReviewStatus.Approved, state: EventState = EventState.BookingOpen,
            mode: AttendanceMode = AttendanceMode.ArrivalOnly, poster: PosterArt = PosterArt.Teal,
            featured: Boolean = false, comment: String? = null, cancelReason: String? = null,
        ) = Event(
            id = id, clubId = club, clubName = clubNames.getValue(club), title = title, description = description,
            start = start, end = start.plusHours(hours), venue = venue, capacity = capacity, bookedCount = booked,
            tags = tags, reviewStatus = review, reviewComment = comment, state = state, attendanceMode = mode,
            pointsRequested = true, pointsJustification = "Open to all students and builds a transferable skill.",
            organiserContact = clubContacts.getValue(club), poster = poster, featured = featured, cancelReason = cancelReason,
            submittedAt = start.minusDays(10),
        )

        val events = listOf(
            event(
                LIVE_EVENT, "cs-society", "Leadership Workshop", liveStart, 2, "DEWAN 2", 30, 22, listOf("Leadership", "Career"),
                "Practise running a team meeting, giving feedback and leading a small project, with student leaders from five societies.",
                state = EventState.CheckInOpen, mode = AttendanceMode.ArrivalAndCheckpoint, poster = PosterArt.Gold,
            ),
            event(
                "build-with-ai", "cs-society", "Build with AI", at(3, "14:00"), 2, "F1A02", 40, 28, listOf("Tech", "Career"),
                "A hands-on workshop: build a small study helper with an AI API, then talk through what it gets wrong and why.",
                mode = AttendanceMode.ArrivalAndCheckpoint, poster = PosterArt.Photo(R.drawable.poster_featured_photo), featured = true,
            ),
            event(
                "python-next-steps", "cs-society", "Python: Next Steps", at(5, "15:00"), 2, "TCR1", 35, 19, listOf("Tech"),
                "For students who finished Intro to Python: files, testing and a small data project you can put on your CV.",
            ),
            event(
                "tech-talk-android", "cs-society", "Tech Talk: Android in Industry", at(5, "16:00"), 2, "F2A04", 60, 30, listOf("Tech", "Career"),
                "Two alumni explain how they ship Android apps at work, and what they look for when hiring interns.",
                poster = PosterArt.Photo(R.drawable.poster_ai_photo),
            ),
            event(
                "garden-volunteering", "rotaract", "Semenyih Community Garden Volunteering Morning", at(6, "08:30"), 4,
                "Community garden, bus from Main Gate", 25, 25, listOf("Volunteering"),
                "Plant, weed and build raised beds with the Semenyih community. Transport, gloves and breakfast provided.",
                poster = PosterArt.Gold,
            ),
            event(
                "open-mic", "arts-club", "Open Mic Night", at(4, "19:30"), 2, "Café Pulse", 50, 12, listOf("Arts"),
                "Sing, read or play something for five minutes. First-timers welcome; sign up for a slot at the door.",
                poster = PosterArt.Gold,
            ),
            event(
                "five-a-side", "sports", "Five-a-side Futsal Cup", at(8, "17:00"), 3, "Sports Complex", 48, 20, listOf("Sports"),
                "Teams of five, mixed levels. Bring a friend or join a team on the night.",
            ),
            event(
                "career-fair", "cs-society", "Tech Career Fair", at(7, "10:00"), 4, "Great Hall", 200, 0, listOf("Career"),
                "Meet recruiters from twelve companies hiring interns and graduates.",
                state = EventState.Cancelled, cancelReason = "The venue is closed for maintenance. We will announce a new date soon.",
            ),
            event(
                "prototype-playground", "cs-society", "Prototype Playground", at(9, "16:00"), 2, "F2A04", 30, 0, listOf("Tech"),
                "Bring an idea and leave with a clickable prototype. Mentors from the Design Society help each team.",
                review = ReviewStatus.PendingReview,
            ),
            event(
                "campus-hack-night", "cs-society", "Campus Hack Night", at(12, "18:00"), 3, "F4C08", 60, 0, listOf("Tech", "Leadership"),
                "An evening hackathon on campus problems, judged by staff from Estates and the Students' Association.",
                review = ReviewStatus.ChangesRequested, comment = "Venue approval needed. Upload the booking confirmation and resubmit.",
            ),
            event(
                "ai-ethics", "cs-society", "AI Ethics Roundtable", at(14, "13:00"), 2, "Library Seminar Room", 25, 0, listOf("Tech", "Leadership"),
                "A guided discussion on fairness and privacy in campus AI tools.", review = ReviewStatus.Draft,
            ),
            event(
                "crypto-meetup", "cs-society", "Crypto Trading Meetup", at(10, "20:00"), 2, "F1A02", 40, 0, listOf("Career"),
                "Share trading tips.", review = ReviewStatus.Rejected, comment = "Not eligible for s-CPD points: no learning outcome.",
            ),
            // Past events for the history, progress and streak screens.
            event("intro-python", "cs-society", "Intro to Python", weeksAgo(1, 2, "15:00"), 2, "TCR1", 35, 30, listOf("Tech"), "Your first Python programs."),
            event(
                "design-sprint", "arts-club", "Poster Design Sprint", weeksAgo(1, 4, "14:00"), 2, "Arts Studio", 20, 18, listOf("Arts"),
                "Design a poster for a real campus event in two hours.", mode = AttendanceMode.ArrivalAndCheckpoint, poster = PosterArt.Gold,
            ),
            event("volunteer-day", "rotaract", "Volunteer Day", weeksAgo(2, 5, "09:00"), 4, "Main Gate", 40, 35, listOf("Volunteering"), "Clean-up at the lake.", poster = PosterArt.Gold),
            event(
                "career-talk", "cs-society", "Careers in Tech Talk", weeksAgo(3, 3, "14:00"), 2, "F1A02", 80, 61, listOf("Career", "Tech"),
                "Graduates talk about their first year at work.", mode = AttendanceMode.ArrivalAndCheckpoint,
            ),
            event("debate-night", "cs-society", "Debate Night", weeksAgo(3, 1, "19:00"), 2, "DEWAN 1", 50, 40, listOf("Leadership"), "Friendly debate on tech policy."),
            event("sports-day", "sports", "Sports Day", weeksAgo(4, 5, "08:00"), 6, "Field", 100, 90, listOf("Sports"), "Relay, tug of war and more."),
        ).map { if (!it.end.isAfter(now.atZone(zone)) && it.reviewStatus == ReviewStatus.Approved && !it.isCancelled) it.copy(state = EventState.CheckInClosed) else it }

        val accounts = buildList {
            add(FakeBackend.Account(UserProfile("u-aisha", "Aisha Rahman", STUDENT_1, AccountRole.Student, "20612345", interests = listOf("Tech", "Career"), hasChosenInterests = true), PASSWORD))
            add(FakeBackend.Account(UserProfile("u-daniel", "Daniel Lim", STUDENT_2, AccountRole.Student, "20654321", interests = listOf("Sports", "Volunteering")), PASSWORD))
            add(FakeBackend.Account(UserProfile("u-cs", "Computer Science Society", COMMITTEE_TECH, AccountRole.Committee, studentId = "20692001", clubIds = listOf("cs-society"), interests = listOf("Tech", "Career"), hasChosenInterests = true), PASSWORD))
            add(FakeBackend.Account(UserProfile("u-arts", "Arts Club", COMMITTEE_ARTS, AccountRole.Committee, studentId = "20692002", clubIds = listOf("arts-club"), interests = listOf("Arts"), hasChosenInterests = true), PASSWORD))
            classmates.forEachIndexed { i, name ->
                add(FakeBackend.Account(UserProfile("u-c$i", name, "c$i@scpd.example", AccountRole.Student, "2060${1000 + i}"), "not-used"))
            }
        }

        val created = now.minus(5, ChronoUnit.DAYS)
        var n = 0
        fun booking(user: String, eventId: String, status: BookingStatus = BookingStatus.Booked, byOrganiser: Boolean = false) =
            Booking("b-${n++}", eventId, user, status, created.plusSeconds(n.toLong()), cancelledByOrganiser = byOrganiser)

        val bookings = buildList {
            listOf(LIVE_EVENT, "python-next-steps", "intro-python", "design-sprint", "volunteer-day", "career-talk", "debate-night")
                .forEach { add(booking("u-aisha", it)) }
            add(booking("u-aisha", "sports-day", BookingStatus.NoShow))
            add(booking("u-aisha", "career-fair", BookingStatus.Cancelled, byOrganiser = true))
            add(booking("u-daniel", LIVE_EVENT))
            add(booking("u-daniel", "garden-volunteering"))
            classmates.indices.forEach { add(booking("u-c$it", LIVE_EVENT)) }
        }

        val evidence = buildMap {
            fun put(user: String, event: String, arrival: StageStatus, checkpoint: StageStatus = StageStatus.NOT_SCANNED, reason: String? = null, flag: String? = null) =
                put("$user|$event", FakeBackend.Evidence(arrival, checkpoint, reason, flag))
            put("u-aisha", "intro-python", StageStatus.VERIFIED)
            put("u-aisha", "design-sprint", StageStatus.VERIFIED)
            put("u-aisha", "volunteer-day", StageStatus.VERIFIED)
            put("u-aisha", "career-talk", StageStatus.VERIFIED, StageStatus.VERIFIED)
            put("u-aisha", "debate-night", StageStatus.REJECTED, reason = "Office review: this phone checked in two accounts.")
            // Classmates already in the room at the live event.
            (0 until 13).forEach { put("u-c$it", LIVE_EVENT, StageStatus.VERIFIED) }
            put("u-c13", LIVE_EVENT, StageStatus.VERIFIED, flag = "Same phone used by another account")
        }

        fun entry(event: String, points: Int, reason: String): FakeBackend.LedgerRow {
            val e = events.first { it.id == event }
            return FakeBackend.LedgerRow("u-aisha", PointsEntry("p-${event}-$points", event, e.title, e.start.toLocalDate(), points, reason))
        }
        val ledger = listOf(
            entry("intro-python", 3, "Verified attendance"),
            entry("volunteer-day", 3, "Verified attendance"),
            entry("career-talk", 3, "Verified attendance"),
            entry("debate-night", 3, "Verified attendance"),
            entry("debate-night", -3, "Office review: this phone checked in two accounts."),
        )

        val sessions = events.filter { it.state == EventState.CheckInOpen || it.state == EventState.CheckInClosed }
            .associate { e ->
                e.id to FakeBackend.Session(e.start.toInstant(), if (e.state == EventState.CheckInClosed) e.end.toInstant() else null)
            }

        return FakeSeed(accounts, events, bookings, evidence, ledger, sessions)
    }
}
