package fr.essence.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import fr.essence.app.ui.theme.PriceColors
import fr.essence.core.model.Station
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Locale

private val FRANCE_CENTER = OsmPoint(46.6, 2.4)

/**
 * Carte OpenStreetMap (osmdroid) avec une bulle de prix par station, colorée du moins cher
 * (vert) au plus cher (rouge) ; la meilleure porte une étoile, les ruptures sont grisées.
 * La carte ne bouge que sur demande explicite ([StationsUiState.camera]).
 */
@Composable
fun StationsMap(
    state: StationsUiState,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 5.0
            controller.setZoom(6.0)
            controller.setCenter(FRANCE_CENTER)
        }
    }
    val bubbles = remember { BubbleFactory(context) }
    val lastCamera = remember { longArrayOf(-1) }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            // Tuiles inversées en mode sombre pour ne pas éblouir la nuit.
            map.overlayManager.tilesOverlay.setColorFilter(if (dark) TilesOverlay.INVERTED_COLORS else null)

            map.overlays.clear()
            val bestId = state.best?.station?.id
            val focusedId = state.focused?.id
            // Les stations en rupture d'abord, pour que les bulles colorées passent au-dessus.
            state.stations
                .sortedWith(compareBy<Station> { it.hasInStock(state.fuel) }.thenBy { it.id == focusedId })
                .forEach { station ->
                    map.overlays.add(
                        stationMarker(map, bubbles, station, state, isBest = station.id == bestId,
                            isFocused = station.id == focusedId, onStationClick),
                    )
                }
            state.position?.let { me ->
                map.overlays.add(Marker(map).apply {
                    position = OsmPoint(me.latitude, me.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = bubbles.myLocation()
                    infoWindow = null
                    setOnMarkerClickListener { _, _ -> true }
                })
            }

            state.camera?.takeIf { it.id != lastCamera[0] }?.let { target ->
                lastCamera[0] = target.id
                val point = OsmPoint(target.point.latitude, target.point.longitude)
                if (target.zoom != null) map.controller.animateTo(point, target.zoom, 600L)
                else map.controller.animateTo(point)
            }
            map.invalidate()
        },
    )
}

private fun stationMarker(
    map: MapView,
    bubbles: BubbleFactory,
    station: Station,
    state: StationsUiState,
    isBest: Boolean,
    isFocused: Boolean,
    onClick: (Station) -> Unit,
) = Marker(map).apply {
    position = OsmPoint(station.position.latitude, station.position.longitude)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    val price = station.priceOf(state.fuel)
    val inStock = station.hasInStock(state.fuel)
    val color = when {
        !inStock || price == null -> PriceColors.OutOfStock
        else -> state.stats?.let { PriceColors.of(it.tierOf(price)) } ?: PriceColors.Medium
    }
    val label = when {
        !inStock -> "Rupture"
        else -> String.format(Locale.FRANCE, "%.3f", price)
    }
    icon = bubbles.priceBubble(if (isBest) "★ $label" else label, color.toArgb(), isFocused)
    // Pas de bulle d'info osmdroid : la fiche en bas de l'écran la remplace.
    infoWindow = null
    setOnMarkerClickListener { _, _ ->
        onClick(station)
        true
    }
}

/** Dessine et met en cache les bulles de prix (texte blanc sur pastille colorée avec une pointe). */
private class BubbleFactory(private val context: Context) {

    private val density = context.resources.displayMetrics.density
    private val cache = HashMap<Triple<String, Int, Boolean>, Drawable>()
    private var myLocation: Drawable? = null

    fun priceBubble(text: String, color: Int, highlighted: Boolean): Drawable =
        cache.getOrPut(Triple(text, color, highlighted)) { draw(text, color, highlighted) }

    private fun draw(text: String, color: Int, highlighted: Boolean): Drawable {
        val scale = if (highlighted) 1.2f else 1f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = 13 * density * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val padH = 8 * density * scale
        val padV = 4 * density * scale
        val pointer = 6 * density * scale
        val border = (if (highlighted) 3 else 1.5f) * density
        val metrics = textPaint.fontMetrics
        val bodyW = textPaint.measureText(text) + 2 * padH
        val bodyH = (metrics.descent - metrics.ascent) + 2 * padV
        val width = (bodyW + 2 * border).toInt() + 1
        val height = (bodyH + pointer + 2 * border).toInt() + 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val body = RectF(border, border, border + bodyW, border + bodyH)
        val radius = bodyH / 2
        val tip = Path().apply {
            moveTo(width / 2f - pointer, body.bottom - 1)
            lineTo(width / 2f, body.bottom + pointer)
            lineTo(width / 2f + pointer, body.bottom - 1)
            close()
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (highlighted) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = border * 2
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(body, radius, radius, outline)
        canvas.drawPath(tip, outline)
        canvas.drawRoundRect(body, radius, radius, fill)
        canvas.drawPath(tip, fill)
        canvas.drawText(text, body.left + padH, body.top + padV - metrics.ascent, textPaint)
        return BitmapDrawable(context.resources, bitmap)
    }

    /** Point bleu « vous êtes ici » avec halo. */
    fun myLocation(): Drawable = myLocation ?: run {
        val size = (28 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.argb(60, 26, 115, 232)
        canvas.drawCircle(c, c, c, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(c, c, 8 * density, paint)
        paint.color = android.graphics.Color.rgb(26, 115, 232)
        canvas.drawCircle(c, c, 6 * density, paint)
        BitmapDrawable(context.resources, bitmap).also { myLocation = it }
    }
}
