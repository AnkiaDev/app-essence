package fr.essence.core

import fr.essence.core.domain.PriceStats
import fr.essence.core.domain.PriceTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceStatsTest {

    private val stats = PriceStats.of(listOf(1.60, 1.70, 1.80, 1.90))!!

    @Test
    fun `calcule min, max et moyenne`() {
        assertEquals(1.60, stats.min)
        assertEquals(1.90, stats.max)
        assertEquals(1.75, stats.average, 1e-9)
        assertEquals(4, stats.count)
    }

    @Test
    fun `classe les prix par tiers`() {
        assertEquals(PriceTier.CHEAP, stats.tierOf(1.60))
        assertEquals(PriceTier.MEDIUM, stats.tierOf(1.75))
        assertEquals(PriceTier.EXPENSIVE, stats.tierOf(1.90))
    }

    @Test
    fun `des prix identiques sont tous moyens`() {
        assertEquals(PriceTier.MEDIUM, PriceStats.of(listOf(1.7, 1.7))!!.tierOf(1.7))
    }

    @Test
    fun `economie sur un plein par rapport a la moyenne`() {
        assertEquals(6.0, stats.savingsVsAverage(1.60), 1e-9)
        assertEquals(-3.0, stats.savingsVsAverage(1.90, liters = 20.0), 1e-9)
    }

    @Test
    fun `aucun prix`() = assertNull(PriceStats.of(emptyList()))
}
