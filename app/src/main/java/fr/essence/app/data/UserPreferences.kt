package fr.essence.app.data

import android.content.Context
import fr.essence.core.model.Fuel

/** Carburant et rayon choisis, conservés entre deux lancements. */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("essence", Context.MODE_PRIVATE)

    var fuel: Fuel
        get() = prefs.getString(KEY_FUEL, null)
            ?.let { name -> Fuel.entries.firstOrNull { it.name == name } }
            ?: Fuel.GAZOLE
        set(value) = prefs.edit().putString(KEY_FUEL, value.name).apply()

    var radiusKm: Double
        get() = prefs.getFloat(KEY_RADIUS, DEFAULT_RADIUS_KM.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_RADIUS, value.toFloat()).apply()

    companion object {
        const val DEFAULT_RADIUS_KM = 10.0
        private const val KEY_FUEL = "fuel"
        private const val KEY_RADIUS = "radius_km"
    }
}
