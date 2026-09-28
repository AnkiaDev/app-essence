package fr.essence.core

import fr.essence.core.domain.BestStationFinder
import fr.essence.core.model.Fuel
import fr.essence.core.model.FuelPrice
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BestStationFinderTest {

    private val home = GeoPoint(48.8566, 2.3522)

    private fun station(id: Long, lat: Double, price: Double, rupture: Boolean = false) = Station(
        id = id,
        position = GeoPoint(lat, 2.3522),
        address = "", postalCode = "", city = "",
        prices = mapOf(Fuel.GAZOLE to FuelPrice(price, null)),
        outOfStock = if (rupture) setOf(Fuel.GAZOLE) else emptySet(),
        open24h = false,
    )

    @Test
    fun `ignore les stations en rupture`() {
        val cheapButEmpty = station(1, 48.86, 1.50, rupture = true)
        val ok = station(2, 48.87, 1.70)
        assertEquals(2, BestStationFinder().best(listOf(cheapButEmpty, ok), home, Fuel.GAZOLE)?.station?.id)
    }

    @Test
    fun `une station un peu moins chere mais trop loin ne gagne pas`() {
        val near = station(1, 48.8600, 1.700) // ~0,4 km
        val far = station(2, 49.0400, 1.690) // ~20 km, 1 centime de moins
        assertEquals(1, BestStationFinder().best(listOf(near, far), home, Fuel.GAZOLE)?.station?.id)
    }

    @Test
    fun `une vraie economie justifie le detour`() {
        val near = station(1, 48.8600, 1.850)
        val far = station(2, 48.9000, 1.650) // ~5 km, 20 centimes de moins
        assertEquals(2, BestStationFinder().best(listOf(near, far), home, Fuel.GAZOLE)?.station?.id)
    }

    @Test
    fun `aucune station approvisionnee`() {
        assertNull(BestStationFinder().best(listOf(station(1, 48.86, 1.7, rupture = true)), home, Fuel.GAZOLE))
    }
}
