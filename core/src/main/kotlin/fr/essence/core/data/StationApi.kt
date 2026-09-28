package fr.essence.core.data

import fr.essence.core.model.Fuel
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Client du flux instantané officiel (mis à jour toutes les 10 minutes,
 * Licence Ouverte 2.0) : https://data.economie.gouv.fr/explore/dataset/prix-des-carburants-en-france-flux-instantane-v2/
 */
class StationApi(engine: HttpClientEngine) {

    private val client = HttpClient(engine) { configure() }

    /**
     * Stations dans un rayon de [radiusKm] autour de [center]. Si [fuel] est fourni,
     * seules les stations publiant un prix pour ce carburant sont renvoyées.
     * L'API limite une page à 100 résultats : on pagine jusqu'à [maxResults].
     */
    suspend fun stationsAround(
        center: GeoPoint,
        radiusKm: Double,
        fuel: Fuel? = null,
        maxResults: Int = 300,
    ): List<Station> {
        val where = buildWhere(center, radiusKm, fuel)
        val stations = mutableListOf<Station>()
        var offset = 0
        while (offset < maxResults) {
            val page: RecordsResponse = client.get(RECORDS_PATH) {
                parameter("where", where)
                parameter("limit", PAGE_SIZE)
                parameter("offset", offset)
            }.body()
            stations += page.results.mapNotNull { it.toStation() }
            offset += PAGE_SIZE
            if (page.results.size < PAGE_SIZE || offset >= page.totalCount) break
        }
        return stations
    }

    fun close() = client.close()

    private fun HttpClientConfig<*>.configure() {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        defaultRequest { url(BASE_URL) }
    }

    companion object {
        const val BASE_URL = "https://data.economie.gouv.fr/api/explore/v2.1/"
        const val RECORDS_PATH =
            "catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records"
        private const val PAGE_SIZE = 100

        internal val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        /** Clause ODSQL : `within_distance(geom, geom'POINT(lon lat)', 10km)`. */
        internal fun buildWhere(center: GeoPoint, radiusKm: Double, fuel: Fuel?): String {
            val geo = String.format(
                Locale.ROOT,
                "within_distance(geom, geom'POINT(%.6f %.6f)', %.1fkm)",
                center.longitude, center.latitude, radiusKm,
            )
            return if (fuel == null) geo else "$geo and ${fuel.apiKey}_prix is not null"
        }
    }
}
