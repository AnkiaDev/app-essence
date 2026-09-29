package fr.essence.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.essence.app.data.UserPreferences
import fr.essence.app.location.LocationProvider
import fr.essence.core.data.StationApi
import fr.essence.core.domain.BestStationFinder
import fr.essence.core.domain.PriceStats
import fr.essence.core.domain.RankedStation
import fr.essence.core.model.Fuel
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Demande de déplacement de la carte ; [id] change à chaque demande pour la rejouer une seule fois. */
data class CameraTarget(val point: GeoPoint, val zoom: Double?, val id: Long)

data class StationsUiState(
    val fuel: Fuel = Fuel.GAZOLE,
    val radiusKm: Double = UserPreferences.DEFAULT_RADIUS_KM,
    val position: GeoPoint? = null,
    val stations: List<Station> = emptyList(),
    /** Stations approvisionnées, de la plus intéressante à la moins intéressante. */
    val ranking: List<RankedStation> = emptyList(),
    val stats: PriceStats? = null,
    val selectedId: Long? = null,
    val loading: Boolean = false,
    val error: String? = null,
    /** Heure (epoch ms) du dernier chargement réussi. */
    val loadedAtMillis: Long? = null,
    val camera: CameraTarget? = null,
) {
    val best: RankedStation? get() = ranking.firstOrNull()

    /** Station affichée dans la fiche : celle touchée sur la carte ou la liste, sinon la meilleure. */
    val focused: Station? get() = selectedId?.let { id -> stations.firstOrNull { it.id == id } } ?: best?.station

    fun rankedOf(station: Station): RankedStation? = ranking.firstOrNull { it.station.id == station.id }

    val outOfStockCount: Int get() = stations.count { fuel in it.outOfStock }
}

class StationsViewModel(app: Application) : AndroidViewModel(app) {

    private val api = StationApi(Android.create())
    private val locationProvider = LocationProvider(app)
    private val finder = BestStationFinder()
    private val preferences = UserPreferences(app)

    private val _state = MutableStateFlow(
        StationsUiState(fuel = preferences.fuel, radiusKm = preferences.radiusKm),
    )
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var cameraRequests = 0L

    /** À appeler une fois la permission de localisation accordée. Annule un chargement en cours. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val position = locationProvider.currentPosition()
                    ?: error("Position indisponible : activez la localisation du téléphone.")
                val current = _state.value
                val stations = api.stationsAround(position, current.radiusKm, current.fuel)
                val ranking = finder.rank(stations, position, current.fuel)
                val firstLoad = current.position == null
                _state.update {
                    it.copy(
                        position = position,
                        stations = stations,
                        ranking = ranking,
                        stats = PriceStats.of(ranking.map(RankedStation::pricePerLiter)),
                        selectedId = it.selectedId?.takeIf { id -> stations.any { s -> s.id == id } },
                        loading = false,
                        loadedAtMillis = System.currentTimeMillis(),
                        camera = if (firstLoad) cameraTo(position, zoom = 14.0) else it.camera,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    /** Rafraîchit seulement si les données ont plus de [maxAgeMillis] (retour dans l'appli). */
    fun refreshIfStale(maxAgeMillis: Long = 10 * 60_000L) {
        if (_state.value.loading) return
        val loadedAt = _state.value.loadedAtMillis
        if (loadedAt == null || System.currentTimeMillis() - loadedAt > maxAgeMillis) refresh()
    }

    fun selectFuel(fuel: Fuel) {
        if (fuel == _state.value.fuel) return
        preferences.fuel = fuel
        _state.update { it.copy(fuel = fuel, selectedId = null) }
        refresh()
    }

    fun selectRadius(radiusKm: Double) {
        if (radiusKm == _state.value.radiusKm) return
        preferences.radiusKm = radiusKm
        _state.update { it.copy(radiusKm = radiusKm) }
        refresh()
    }

    /** Sélectionne une station ; [moveCamera] quand le choix vient de la liste et non de la carte. */
    fun selectStation(id: Long?, moveCamera: Boolean = false) {
        _state.update { state ->
            val station = id?.let { state.stations.firstOrNull { it.id == id } }
            state.copy(
                selectedId = id,
                camera = if (moveCamera && station != null) cameraTo(station.position, zoom = null) else state.camera,
            )
        }
    }

    fun recenter() {
        val position = _state.value.position ?: return refresh()
        _state.update { it.copy(camera = cameraTo(position, zoom = 14.0)) }
    }

    private fun cameraTo(point: GeoPoint, zoom: Double?) = CameraTarget(point, zoom, ++cameraRequests)

    private fun Exception.toUserMessage(): String = when (this) {
        is IOException -> "Pas de connexion au service des prix. Vérifiez votre réseau."
        is IllegalStateException -> message ?: "Erreur inattendue"
        else -> "Le service des prix ne répond pas correctement (${this::class.simpleName})."
    }

    override fun onCleared() {
        api.close()
    }
}
