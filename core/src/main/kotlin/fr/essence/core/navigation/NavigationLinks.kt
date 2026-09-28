package fr.essence.core.navigation

import fr.essence.core.model.GeoPoint
import java.util.Locale

/** Liens profonds pour lancer un guidage vers une station. */
object NavigationLinks {

    /** Ouvre Waze en navigation ; si Waze n'est pas installé, le lien s'ouvre dans le navigateur. */
    fun waze(destination: GeoPoint): String =
        "https://waze.com/ul?ll=${destination.coords()}&navigate=yes"

    /** Intent natif de Google Maps qui démarre directement le guidage en voiture. */
    fun googleMapsNavigation(destination: GeoPoint): String =
        "google.navigation:q=${destination.coords()}&mode=d"

    /** Repli universel (navigateur ou toute appli de cartes). */
    fun googleMapsWeb(destination: GeoPoint): String =
        "https://www.google.com/maps/dir/?api=1&destination=${destination.coords()}&travelmode=driving"

    private fun GeoPoint.coords() =
        String.format(Locale.ROOT, "%.6f,%.6f", latitude, longitude)
}
