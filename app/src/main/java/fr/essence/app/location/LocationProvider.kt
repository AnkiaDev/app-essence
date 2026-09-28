package fr.essence.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import fr.essence.core.model.GeoPoint
import kotlinx.coroutines.tasks.await

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Position actuelle ; l'appelant doit avoir obtenu la permission de localisation. */
    @SuppressLint("MissingPermission")
    suspend fun currentPosition(): GeoPoint? {
        val location = client.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token,
        ).await() ?: client.lastLocation.await()
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }
}
