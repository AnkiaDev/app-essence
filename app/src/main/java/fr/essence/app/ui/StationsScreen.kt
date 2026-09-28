package fr.essence.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.essence.app.Navigator
import fr.essence.core.domain.RankedStation
import fr.essence.core.model.Fuel
import java.util.Locale

@Composable
fun StationsScreen(viewModel: StationsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) viewModel.refresh() else permissionDenied = true
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FuelSelector(selected = state.fuel, onSelect = viewModel::selectFuel)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Box(Modifier.weight(1f)) {
                StationsMap(state, onStationClick = {}, modifier = Modifier.fillMaxSize())
            }
            when {
                permissionDenied -> Message("La localisation est nécessaire pour trouver les stations proches.")
                state.error != null -> Message(state.error!!)
                state.best != null -> BestStationCard(state.best!!, state.fuel)
                !state.loading && state.position != null ->
                    Message("Aucune station approvisionnée en ${state.fuel.label} à moins de ${state.radiusKm.toInt()} km.")
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
            FilterChip(selected = fuel == selected, onClick = { onSelect(fuel) }, label = { Text(fuel.label) })
        }
    }
}

@Composable
private fun BestStationCard(best: RankedStation, fuel: Fuel) {
    val context = LocalContext.current
    val station = best.station
    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Meilleure station", style = MaterialTheme.typography.labelMedium)
            Text(
                String.format(Locale.FRANCE, "%s %.3f €/L · %.1f km", fuel.label, best.pricePerLiter, best.distanceKm),
                style = MaterialTheme.typography.titleLarge,
            )
            Text("${station.address}, ${station.postalCode} ${station.city}".trim(' ', ','))
            if (station.open24h) Text("Automate 24h/24", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { Navigator.openWaze(context, station.position) }) { Text("Waze") }
                OutlinedButton(onClick = { Navigator.openGoogleMaps(context, station.position) }) { Text("Google Maps") }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
}
