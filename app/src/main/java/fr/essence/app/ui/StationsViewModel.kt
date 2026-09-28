package fr.essence.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.essence.app.location.LocationProvider
import fr.essence.core.data.StationApi
import fr.essence.core.domain.BestStationFinder
import fr.essence.core.domain.RankedStation
import fr.essence.core.model.Fuel
import fr.essence.core.model.GeoPoint
import fr.essence.core.model.Station
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationsUiState(
    val fuel: Fuel = Fuel.GAZOLE,
    val radiusKm: Double = 10.0,
    val position: GeoPoint? = null,
    val stations: List<Station> = emptyList(),
    /** Stations approvisionnées, de la plus intéressante à la moins intéressante. */
    val ranking: List<RankedStation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val best: RankedStation? get() = ranking.firstOrNull()
}

class StationsViewModel(app: Application) : AndroidViewModel(app) {

    private val api = StationApi(Android.create())
    private val locationProvider = LocationProvider(app)
    private val finder = BestStationFinder()

    private val _state = MutableStateFlow(StationsUiState())
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    /** À appeler une fois la permission de localisation accordée. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val position = locationProvider.currentPosition()
                    ?: error("Position indisponible : activez la localisation.")
                val current = _state.value
                val stations = api.stationsAround(position, current.radiusKm, current.fuel)
                _state.update {
                    it.copy(
                        position = position,
                        stations = stations,
                        ranking = finder.rank(stations, position, it.fuel),
                        loading = false,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Erreur réseau") }
            }
        }
    }

    fun selectFuel(fuel: Fuel) {
        if (fuel == _state.value.fuel) return
        _state.update { it.copy(fuel = fuel) }
        refresh()
    }

    override fun onCleared() {
        api.close()
    }
}
