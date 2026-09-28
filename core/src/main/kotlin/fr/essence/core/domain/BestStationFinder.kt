package fr.essence.core.domain

import fr.essence.core.model.Fuel
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station

data class RankedStation(
    val station: Station,
    val pricePerLiter: Double,
    val distanceKm: Double,
    /** Coût estimé du plein, trajet aller-retour jusqu'à la station compris. */
    val estimatedCost: Double,
)

/**
 * Choisit la station « la moins chère la plus proche » en ramenant prix et distance
 * à une seule grandeur : ce que coûte réellement d'aller y faire le plein.
 *
 * coût = prix × litres du plein + prix × conso × distance aller-retour
 *
 * Une station 3 centimes moins chère mais à 15 km ne gagne donc que si l'économie
 * couvre le carburant brûlé pour y aller. Les distances sont à vol d'oiseau,
 * multipliées par [roadFactor] pour approcher la distance routière.
 */
class BestStationFinder(
    private val tankLiters: Double = 40.0,
    private val consumptionPer100Km: Double = 6.5,
    private val roadFactor: Double = 1.3,
) {
    fun rank(stations: List<Station>, from: GeoPoint, fuel: Fuel): List<RankedStation> =
        stations
            .filter { it.hasInStock(fuel) }
            .map { station ->
                val price = station.priceOf(fuel)!!
                val distance = from.distanceKmTo(station.position) * roadFactor
                val tripLiters = consumptionPer100Km / 100.0 * distance * 2
                RankedStation(station, price, distance, price * (tankLiters + tripLiters))
            }
            .sortedBy { it.estimatedCost }

    fun best(stations: List<Station>, from: GeoPoint, fuel: Fuel): RankedStation? =
        rank(stations, from, fuel).firstOrNull()
}
