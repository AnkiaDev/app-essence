package fr.essence.core

import fr.essence.core.domain.Freshness
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreshnessTest {

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Test
    fun `lit les horodatages avec ou sans fuseau`() {
        assertEquals(Instant.parse("2026-09-28T08:12:00Z"), Freshness.parse("2026-09-28T08:12:00+00:00"))
        // Sans fuseau : heure de Paris (UTC+2 en septembre).
        assertEquals(Instant.parse("2026-09-28T06:12:00Z"), Freshness.parse("2026-09-28 08:12:00"))
        assertNull(Freshness.parse("pas une date"))
        assertNull(Freshness.parse(null))
    }

    @Test
    fun `libelle d'anciennete`() {
        assertEquals("à l'instant", Freshness.ageLabel(now.minusSeconds(20), now))
        assertEquals("il y a 12 min", Freshness.ageLabel(now.minusSeconds(12 * 60), now))
        assertEquals("il y a 3 h", Freshness.ageLabel(now.minusSeconds(3 * 3600 + 600), now))
        assertEquals("il y a 2 j", Freshness.ageLabel(now.minusSeconds(2 * 86400 + 60), now))
        assertEquals("à l'instant", Freshness.ageLabel(now.plusSeconds(300), now))
    }

    @Test
    fun `un prix de plus de trois jours est douteux`() {
        assertFalse(Freshness.isStale(now.minusSeconds(2 * 86400), now))
        assertTrue(Freshness.isStale(now.minusSeconds(4 * 86400), now))
    }
}
