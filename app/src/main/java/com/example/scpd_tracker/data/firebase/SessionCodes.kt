package com.example.scpd_tracker.data.firebase

import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.Stage
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant

/**
 * The code maths for check-in on the free Firebase plan. It must give exactly the same answers as the Firestore
 * rules (`codeMatches` and `backupCodeMatches` in firebase/firestore.rules) and firebase/test/codes.mjs.
 *
 * QR payload: `SCPD2|eventId|stage|window|sha256hex(secret|eventId|stage|window)`, stage = "arrival" or "checkpoint".
 * Backup code: sha256hex(secret|code|eventId|stage|window), letters a-f turned into 0-5, first 6 characters.
 */
object SessionCodes {
    const val PREFIX = "SCPD2"
    private val windowSeconds = Policy.CODE_ROTATION.seconds

    fun stageKey(stage: Stage): String = stage.name.lowercase()

    fun windowAt(time: Instant): Long = Math.floorDiv(time.toEpochMilli(), windowSeconds * 1000)
    fun windowStart(window: Long): Instant = Instant.ofEpochSecond(window * windowSeconds)

    fun signature(secret: String, eventId: String, stage: Stage, window: Long): String =
        sha256Hex("$secret|$eventId|${stageKey(stage)}|$window")

    fun qrPayload(secret: String, eventId: String, stage: Stage, window: Long): String =
        "$PREFIX|$eventId|${stageKey(stage)}|$window|${signature(secret, eventId, stage, window)}"

    fun backupCode(secret: String, eventId: String, stage: Stage, window: Long): String =
        sha256Hex("$secret|code|$eventId|${stageKey(stage)}|$window")
            .map { c -> if (c in 'a'..'f') '0' + (c - 'a') else c }
            .take(6).joinToString("")

    /** A new random session secret: 32 bytes as 64 hex characters (the rules need at least 32). */
    fun newSecret(random: SecureRandom = SecureRandom()): String =
        ByteArray(32).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    data class ParsedQr(val eventId: String, val stage: String, val window: Long, val signature: String)

    /** Splits a scanned QR, or returns null when it isn't an s-CPD check-in code. The signature is checked by the rules. */
    fun parse(payload: String): ParsedQr? {
        val parts = payload.trim().split('|')
        if (parts.size != 5 || parts[0] != PREFIX) return null
        val window = parts[3].toLongOrNull() ?: return null
        if (parts[2] !in setOf("arrival", "checkpoint") || !parts[4].matches(Regex("[0-9a-f]{64}"))) return null
        return ParsedQr(parts[1], parts[2], window, parts[4])
    }

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
