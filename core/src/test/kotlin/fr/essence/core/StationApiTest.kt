package fr.essence.core

import fr.essence.core.data.StationApi
import fr.essence.core.model.Fuel
import fr.essence.core.model.GeoPoint
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StationApiTest {

    // Forme d'un enregistrement de l'API Explore v2.1 (champs inconnus ignorés).
    private val sample = """
        {
          "total_count": 2,
          "results": [
            {
              "id": 75012001, "adresse": "12 Avenue Daumesnil", "cp": "75012", "ville": "Paris",
              "geom": {"lon": 2.3802, "lat": 48.8443},
              "horaires_automate_24_24": "Oui",
              "gazole_prix": 1.689, "gazole_maj": "2026-09-28T08:12:00+00:00",
              "e10_prix": 1.759, "e10_maj": "2026-09-28T08:12:00+00:00",
              "sp98_prix": 1.849,
              "carburants_indisponibles": ["SP95", "E85", "GPLc"],
              "carburants_rupture_temporaire": "SP98",
              "services_service": ["Lavage automatique"]
            },
            {
              "id": 1, "adresse": null, "cp": "01000", "ville": "Bourg",
              "gazole_prix": 1.6
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parse les prix, les ruptures et ignore les stations sans position`() = runTest {
        var requestedUrl = ""
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(sample, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val stations = StationApi(engine).stationsAround(GeoPoint(48.85, 2.35), 5.0, Fuel.GAZOLE)

        assertEquals(1, stations.size)
        val s = stations.single()
        assertEquals(1.689, s.priceOf(Fuel.GAZOLE))
        assertTrue(s.hasInStock(Fuel.GAZOLE))
        assertTrue(s.hasInStock(Fuel.E10))
        assertFalse(s.hasInStock(Fuel.SP98), "SP98 est en rupture temporaire")
        assertFalse(s.hasInStock(Fuel.SP95), "SP95 n'est pas proposé")
        assertTrue(s.open24h)
        assertTrue(requestedUrl.startsWith(StationApi.BASE_URL + StationApi.RECORDS_PATH))
        assertTrue("within_distance" in java.net.URLDecoder.decode(requestedUrl, "UTF-8"))
    }

    @Test
    fun `la clause where filtre par rayon et par carburant`() {
        val where = StationApi.buildWhere(GeoPoint(48.8443, 2.3802), 10.0, Fuel.E10)
        assertEquals(
            "within_distance(geom, geom'POINT(2.380200 48.844300)', 10.0km) and e10_prix is not null",
            where,
        )
    }
}
