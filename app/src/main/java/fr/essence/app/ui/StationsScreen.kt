package fr.essence.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.essence.app.Navigator
import fr.essence.app.R
import fr.essence.app.ui.theme.PriceColors
import fr.essence.core.domain.Freshness
import fr.essence.core.domain.RankedStation
import fr.essence.core.model.Fuel
import fr.essence.core.model.Station
import kotlinx.coroutines.launch
import java.time.Instant

private val RADIUS_OPTIONS = listOf(5.0, 10.0, 20.0)
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun Context.hasLocationPermission() = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(viewModel: StationsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        permissionDenied = granted.values.none { it }
        if (!permissionDenied) viewModel.refresh()
    }
    LaunchedEffect(Unit) {
        if (!context.hasLocationPermission()) permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
    // Au retour dans l'appli (ou depuis les réglages), on recharge si les prix ont plus de 10 min.
    LifecycleResumeEffect(Unit) {
        if (context.hasLocationPermission()) {
            permissionDenied = false
            viewModel.refreshIfStale()
        }
        onPauseOrDispose { }
    }
    val refreshOrAsk = {
        if (context.hasLocationPermission()) viewModel.refresh() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val listState = rememberLazyListState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 270.dp,
        sheetContent = {
            StationsSheet(
                state = state,
                permissionDenied = permissionDenied,
                listState = listState,
                onAskPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                onOpenSettings = { context.openAppSettings() },
                onRetry = refreshOrAsk,
                onRadius = viewModel::selectRadius,
                onStationFromList = { station ->
                    viewModel.selectStation(station.id, moveCamera = true)
                    scope.launch {
                        listState.animateScrollToItem(0)
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            StationsMap(
                state = state,
                onStationClick = { station ->
                    viewModel.selectStation(station.id)
                    scope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding()) {
                FuelSelector(selected = state.fuel, onSelect = viewModel::selectFuel)
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 12.dp))
            }
            Column(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 64.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(onClick = { viewModel.recenter() }) {
                    Icon(painterResource(R.drawable.ic_my_location), contentDescription = "Me recentrer")
                }
                SmallFloatingActionButton(onClick = refreshOrAsk) {
                    Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Actualiser les prix")
                }
            }
        }
    }
}

@Composable
private fun FuelSelector(selected: Fuel, onSelect: (Fuel) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Fuel.entries.forEach { fuel ->
            FilterChip(
                selected = fuel == selected,
                onClick = { onSelect(fuel) },
                label = { Text(fuel.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
            )
        }
    }
}

@Composable
private fun StationsSheet(
    state: StationsUiState,
    permissionDenied: Boolean,
    listState: LazyListState,
    onAskPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onRadius: (Double) -> Unit,
    onStationFromList: (Station) -> Unit,
) {
    val now = Instant.now()
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        item {
            val focused = state.focused
            when {
                permissionDenied -> PermissionCard(onAskPermission, onOpenSettings)
                state.error != null -> MessageCard(state.error, actionLabel = "Réessayer", onAction = onRetry)
                focused != null -> StationCard(focused, state, now)
                state.loading || state.position == null ->
                    MessageCard("Recherche des stations autour de vous…")
                else -> {
                    val wider = RADIUS_OPTIONS.firstOrNull { it > state.radiusKm }
                    MessageCard(
                        "Aucune station approvisionnée en ${state.fuel.label} à moins de ${state.radiusKm.toInt()} km.",
                        actionLabel = wider?.let { "Chercher à ${it.toInt()} km" },
                        onAction = { wider?.let(onRadius) },
                    )
                }
            }
        }
        item { RadiusSelector(state.radiusKm, onRadius) }
        item { Summary(state, now) }
        itemsIndexed(state.ranking, key = { _, ranked -> ranked.station.id }) { index, ranked ->
            StationRow(
                ranked = ranked,
                state = state,
                isBest = index == 0,
                selected = ranked.station.id == state.focused?.id,
                onClick = { onStationFromList(ranked.station) },
            )
        }
        item {
            Text(
                "Prix : prix-carburants.gouv.fr (Licence Ouverte 2.0) · Carte © contributeurs OpenStreetMap",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun StationCard(station: Station, state: StationsUiState, now: Instant) {
    val context = LocalContext.current
    val ranked = state.rankedOf(station)
    val best = state.best
    val isBest = ranked != null && ranked.station.id == best?.station?.id
    val colors = if (isBest) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = colors) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isBest) "Meilleure option" else "Station sélectionnée",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (station.open24h) Tag("24h/24")
            }
            if (ranked != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatPrice(ranked.pricePerLiter),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("/L ${state.fuel.label}", Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Spacer(Modifier.weight(1f))
                    Text("≈ ${formatDistance(ranked.distanceKm)}", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    "${state.fuel.label} en rupture ou non proposé ici",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                listOf(station.address, "${station.postalCode} ${station.city}".trim())
                    .filter { it.isNotBlank() }.joinToString(", "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ranked?.let { SavingsLine(it, state, isBest) }
            FreshnessLine(station, state.fuel, now)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { Navigator.openWaze(context, station.position) }, Modifier.weight(1f)) {
                    Text("Waze")
                }
                OutlinedButton(onClick = { Navigator.openGoogleMaps(context, station.position) }, Modifier.weight(1f)) {
                    Text("Google Maps")
                }
            }
        }
    }
}

@Composable
private fun SavingsLine(ranked: RankedStation, state: StationsUiState, isBest: Boolean) {
    val stats = state.stats ?: return
    val best = state.best ?: return
    val text = if (isBest) {
        val saved = stats.savingsVsAverage(ranked.pricePerLiter)
        if (stats.count < 2 || saved < 0.10) return
        "≈ ${formatEuros(saved)} d'économie sur un plein de 40 L par rapport à la moyenne du secteur"
    } else {
        val extra = ranked.estimatedCost - best.estimatedCost
        if (extra < 0.05) return
        "≈ ${formatEuros(extra)} de plus que la meilleure option, trajet compris"
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun FreshnessLine(station: Station, fuel: Fuel, now: Instant) {
    val updated = Freshness.parse(station.prices[fuel]?.updatedAt) ?: return
    val stale = Freshness.isStale(updated, now)
    Text(
        "Prix mis à jour ${Freshness.ageLabel(updated, now)}" + if (stale) " : à vérifier sur place" else "",
        style = MaterialTheme.typography.bodySmall,
        color = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StationRow(
    ranked: RankedStation,
    state: StationsUiState,
    isBest: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val station = ranked.station
    val tierColor = state.stats?.let { PriceColors.of(it.tierOf(ranked.pricePerLiter)) } ?: PriceColors.Medium
    val extra = state.best?.let { ranked.estimatedCost - it.estimatedCost } ?: 0.0
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = if (selected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        leadingContent = {
            Text(
                formatPrice(ranked.pricePerLiter).removeSuffix(" €"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(tierColor, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        },
        headlineContent = {
            Text(station.address.ifBlank { station.city }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                listOf(station.city, "≈ ${formatDistance(ranked.distanceKm)}").filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            when {
                isBest -> Tag("Meilleur")
                extra >= 0.05 -> Text("+${formatEuros(extra)}", style = MaterialTheme.typography.labelMedium)
                else -> {}
            }
        },
    )
    HorizontalDivider()
}

@Composable
private fun RadiusSelector(radiusKm: Double, onSelect: (Double) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Rayon", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        RADIUS_OPTIONS.forEach { km ->
            FilterChip(selected = km == radiusKm, onClick = { onSelect(km) }, label = { Text("${km.toInt()} km") })
        }
    }
}

@Composable
private fun Summary(state: StationsUiState, now: Instant) {
    if (state.position == null) return
    val parts = buildList {
        add("${state.ranking.size} station${if (state.ranking.size > 1) "s" else ""} approvisionnée${if (state.ranking.size > 1) "s" else ""}")
        if (state.outOfStockCount > 0) add("${state.outOfStockCount} en rupture")
        state.loadedAtMillis?.let { add("actualisé ${Freshness.ageLabel(Instant.ofEpochMilli(it), now)}") }
    }
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun PermissionCard(onAsk: () -> Unit, onOpenSettings: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Localisation désactivée", style = MaterialTheme.typography.titleMedium)
            Text("Elle sert uniquement à trouver les stations autour de vous ; rien n'est envoyé ailleurs que pour la recherche.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAsk) { Text("Autoriser") }
                OutlinedButton(onClick = onOpenSettings) { Text("Ouvrir les réglages") }
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text)
            if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
