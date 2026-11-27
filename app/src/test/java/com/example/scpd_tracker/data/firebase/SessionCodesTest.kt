package com.example.scpd_tracker.data.firebase

import com.example.scpd_tracker.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The phone, the Firestore rules and the rules tests must agree on every code. The expected values come from
 * firebase/test/codes.mjs, which the rules tests prove against the emulator.
 */
class SessionCodesTest {
    private val secret = "0123456789abcdef0123456789abcdef"
    private val event = "leadership-workshop"

    @Test fun `QR signature matches the rules`() {
        assertEquals(
            "de260960b26f1372cac3cd07ee9d717880525c5e965b26a42a1fde5c662d3794",
            SessionCodes.signature(secret, event, Stage.Arrival, 59034567),
        )
    }

    @Test fun `backup codes match the rules`() {
        assertEquals("756400", SessionCodes.backupCode(secret, event, Stage.Arrival, 59034567))
        assertEquals("451107", SessionCodes.backupCode(secret, event, Stage.Checkpoint, 59034568))
    }

    @Test fun `a payload parses back to its parts`() {
        val parsed = SessionCodes.parse(SessionCodes.qrPayload(secret, event, Stage.Checkpoint, 42))!!
        assertEquals(event, parsed.eventId)
        assertEquals("checkpoint", parsed.stage)
        assertEquals(42L, parsed.window)
    }

    @Test fun `codes that are not s-CPD codes are refused`() {
        assertNull(SessionCodes.parse("https://example.com"))
        assertNull(SessionCodes.parse("SCPD1|e|Arrival|1|abc"))
        assertNull(SessionCodes.parse("SCPD2|e|arrival|x|" + "a".repeat(64)))
    }

    @Test fun `windows are 30 seconds long`() {
        assertEquals(0L, SessionCodes.windowAt(Instant.ofEpochSecond(29)))
        assertEquals(1L, SessionCodes.windowAt(Instant.ofEpochSecond(30)))
        assertEquals(Instant.ofEpochSecond(60), SessionCodes.windowStart(2))
    }

    @Test fun `new secrets are 64 hex characters`() {
        assertTrue(SessionCodes.newSecret().matches(Regex("[0-9a-f]{64}")))
    }
}
