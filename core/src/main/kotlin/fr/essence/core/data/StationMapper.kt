package fr.essence.core.data

import fr.essence.core.model.Fuel
import fr.essence.core.model.FuelPrice
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Convertit un enregistrement brut en [Station], ou null s'il n'est pas géolocalisé. */
fun StationDto.toStation(): Station? {
    val point = geom ?: return null
    val prices = buildMap {
        fun add(fuel: Fuel, price: Double?, maj: String?) {
            if (price != null && price > 0) put(fuel, FuelPrice(price, maj))
        }
        add(Fuel.GAZOLE, gazolePrix, gazoleMaj)
        add(Fuel.SP95, sp95Prix, sp95Maj)
        add(Fuel.E10, e10Prix, e10Maj)
        add(Fuel.SP98, sp98Prix, sp98Maj)
        add(Fuel.E85, e85Prix, e85Maj)
        add(Fuel.GPLC, gplcPrix, gplcMaj)
    }
    val outOfStock = (fuelNames(carburantsIndisponibles) +
        fuelNames(ruptureTemporaire) +
        fuelNames(ruptureDefinitive))
        .mapNotNull(Fuel::fromApiName)
        .toSet()
    return Station(
        id = id,
        position = GeoPoint(point.lat, point.lon),
        address = adresse.orEmpty(),
        postalCode = cp.orEmpty(),
        city = ville.orEmpty(),
        prices = prices,
        outOfStock = outOfStock,
        open24h = automate24h.equals("Oui", ignoreCase = true),
    )
}

internal fun fuelNames(element: JsonElement?): List<String> = when (element) {
    null, JsonNull -> emptyList()
    is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }
    is JsonPrimitive -> element.contentOrNull?.split(';', ',').orEmpty()
    else -> emptyList()
}.map { it.trim() }.filter { it.isNotEmpty() }
