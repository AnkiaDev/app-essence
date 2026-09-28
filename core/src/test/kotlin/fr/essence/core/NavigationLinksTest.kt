package fr.essence.core

import fr.essence.core.model.GeoPoint
import fr.essence.core.navigation.NavigationLinks
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationLinksTest {
    private val p = GeoPoint(48.8443, 2.3802)

    @Test
    fun waze() = assertEquals("https://waze.com/ul?ll=48.844300,2.380200&navigate=yes", NavigationLinks.waze(p))

    @Test
    fun googleMaps() {
        assertEquals("google.navigation:q=48.844300,2.380200&mode=d", NavigationLinks.googleMapsNavigation(p))
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=48.844300,2.380200&travelmode=driving",
            NavigationLinks.googleMapsWeb(p),
        )
    }
}
