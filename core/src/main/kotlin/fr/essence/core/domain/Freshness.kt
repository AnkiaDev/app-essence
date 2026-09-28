package fr.essence.core.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Ancienneté des prix publiés (`<carburant>_maj`), pour signaler les prix douteux. */
object Freshness {

    /** Au-delà, un prix n'a probablement pas été mis à jour par la station. */
    val STALE_AFTER: Duration = Duration.ofDays(3)

    private val PARIS = ZoneId.of("Europe/Paris")

    /** Lit un horodatage ISO-8601, avec ou sans fuseau (sans fuseau : heure de Paris). */
    fun parse(raw: String?): Instant? {
        val text = raw?.trim()?.replace(' ', 'T')?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            OffsetDateTime.parse(text).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(text).atZone(PARIS).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /** « à l'instant », « il y a 12 min », « il y a 3 h », « il y a 2 j ». */
    fun ageLabel(updatedAt: Instant, now: Instant): String {
        val minutes = Duration.between(updatedAt, now).toMinutes().coerceAtLeast(0)
        return when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "il y a $minutes min"
            minutes < 24 * 60 -> "il y a ${minutes / 60} h"
            else -> "il y a ${minutes / (24 * 60)} j"
        }
    }

    fun isStale(updatedAt: Instant, now: Instant): Boolean =
        Duration.between(updatedAt, now) > STALE_AFTER
}
