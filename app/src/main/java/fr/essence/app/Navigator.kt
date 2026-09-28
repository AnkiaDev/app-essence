package fr.essence.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.essence.core.model.GeoPoint
import fr.essence.core.navigation.NavigationLinks

/** Envoie la station choisie vers Waze ou Google Maps. */
object Navigator {

    fun openWaze(context: Context, destination: GeoPoint) {
        // Le lien universel ouvre Waze s'il est installé, sinon sa page web.
        context.startView(NavigationLinks.waze(destination))
    }

    fun openGoogleMaps(context: Context, destination: GeoPoint) {
        val native = Intent(Intent.ACTION_VIEW, Uri.parse(NavigationLinks.googleMapsNavigation(destination)))
            .setPackage("com.google.android.apps.maps")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(native)
        } catch (_: ActivityNotFoundException) {
            context.startView(NavigationLinks.googleMapsWeb(destination))
        }
    }

    private fun Context.startView(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
