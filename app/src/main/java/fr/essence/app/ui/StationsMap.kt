package fr.essence.app.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import fr.essence.core.domain.RankedStation
import fr.essence.core.model.Fuel
import fr.essence.core.model.Station
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private val PARIS = OsmPoint(48.8566, 2.3522)

/** Carte OpenStreetMap (osmdroid) : vert = meilleure station, bleu = approvisionnée, gris = rupture. */
@Composable
fun StationsMap(
    state: StationsUiState,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(PARIS)
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            val bestId = state.best?.station?.id
            val rankedById = state.ranking.associateBy { it.station.id }
            state.stations.forEach { station ->
                map.overlays.add(
                    stationMarker(map, station, state.fuel, rankedById[station.id], station.id == bestId, onStationClick),
                )
            }
            state.position?.let { me ->
                map.overlays.add(Marker(map).apply {
                    position = OsmPoint(me.latitude, me.longitude)
                    title = "Vous êtes ici"
                    icon = tinted(map, Color.RED)
                })
                if (state.best == null) map.controller.setCenter(OsmPoint(me.latitude, me.longitude))
            }
            state.best?.let { map.controller.animateTo(OsmPoint(it.station.position.latitude, it.station.position.longitude)) }
            map.invalidate()
        },
    )
}

private fun stationMarker(
    map: MapView,
    station: Station,
    fuel: Fuel,
    ranked: RankedStation?,
    isBest: Boolean,
    onClick: (Station) -> Unit,
) = Marker(map).apply {
    position = OsmPoint(station.position.latitude, station.position.longitude)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    title = station.address.ifBlank { station.city }
    snippet = station.priceOf(fuel)
        ?.let { String.format(Locale.FRANCE, "%s : %.3f €/L", fuel.label, it) }
        .let { if (fuel in station.outOfStock) "${fuel.label} : rupture" else it }
    icon = tinted(map, when {
        isBest -> Color.rgb(46, 160, 67)
        ranked != null -> Color.rgb(25, 118, 210)
        else -> Color.GRAY
    })
    setOnMarkerClickListener { marker, _ ->
        marker.showInfoWindow()
        onClick(station)
        true
    }
}

private fun tinted(map: MapView, color: Int) =
    ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.marker_default)!!
        .mutate()
        .apply { setTint(color) }
