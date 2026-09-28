package fr.essence.core.model

data class GeoPoint(val latitude: Double, val longitude: Double)

data class FuelPrice(
    /** Prix en euros par litre. */
    val pricePerLiter: Double,
    /** Horodatage ISO-8601 de la dernière mise à jour publiée, tel que fourni par l'API. */
    val updatedAt: String?,
)

data class Station(
    val id: Long,
    val position: GeoPoint,
    val address: String,
    val postalCode: String,
    val city: String,
    val prices: Map<Fuel, FuelPrice>,
    /** Carburants signalés en rupture (temporaire ou définitive). */
    val outOfStock: Set<Fuel>,
    val open24h: Boolean,
) {
    /** Vrai si la station publie un prix pour ce carburant et ne le signale pas en rupture. */
    fun hasInStock(fuel: Fuel): Boolean = fuel in prices && fuel !in outOfStock

    fun priceOf(fuel: Fuel): Double? = prices[fuel]?.pricePerLiter
}
