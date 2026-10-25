package com.rubydew.nectar.ui.species

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.domain.Region
import com.rubydew.nectar.domain.Species
import com.rubydew.nectar.ui.theme.RaisedCard
import com.rubydew.nectar.ui.theme.RubyDewTheme
import org.osmdroid.views.MapView
import java.text.DateFormat
import java.util.Date

@Composable
fun SpeciesScreen(viewModel: SpeciesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mapView by remember { mutableStateOf<MapView?>(null) }

    SpeciesContent(
        state = state,
        onZipChange = viewModel::onZipChange,
        onLookUp = viewModel::lookUp,
        onLogSighting = { species ->
            mapView?.mapCenter?.let { viewModel.addSighting(species.id, it.latitude, it.longitude) }
        },
        onDeleteSighting = viewModel::deleteSighting,
        map = { modifier ->
            SightingMap(
                region = state.region,
                sightings = state.sightings,
                onMapReady = { mapView = it },
                modifier = modifier,
            )
        },
    )
}

@Composable
fun SpeciesContent(
    state: SpeciesUiState,
    onZipChange: (String) -> Unit,
    onLookUp: () -> Unit,
    onLogSighting: (Species) -> Unit,
    onDeleteSighting: (Long) -> Unit,
    map: @Composable (Modifier) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Species & sightings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.zipInput,
                onValueChange = onZipChange,
                label = { Text("ZIP code") },
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); onLookUp() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { focusManager.clearFocus(); onLookUp() }) { Text("Find birds") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        RaisedCard(modifier = Modifier.fillMaxWidth()) {
            map(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
        }

        LogSightingRow(options = state.loggableSpecies, onLog = onLogSighting)

        Text(
            when {
                state.region == null -> "All U.S. hummingbirds"
                state.species.isEmpty() -> "${state.region.displayName}: no native hummingbirds"
                else -> "Common in ${state.region.displayName} (${state.savedZip})"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (state.region == Region.HAWAII) {
            Text("Hawaii has no native hummingbirds, but you can still log any you spot.")
        }
        state.species.forEach { SpeciesRow(it) }

        Text("Your sightings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (state.sightings.isEmpty()) {
            Text("No sightings yet. Move the map to where you saw a bird, pick the species and tap Log.")
        }
        state.sightings.forEach { SightingRow(it, onDelete = { onDeleteSighting(it.id) }) }

        Text(
            "All features are free. Your sightings are saved only on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogSightingRow(options: List<Species>, onLog: (Species) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = options.firstOrNull { it.id == selectedId } ?: options.first()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Log a sighting at the map center", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected.commonName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { species ->
                        DropdownMenuItem(
                            text = { Text(species.commonName) },
                            onClick = {
                                selectedId = species.id
                                expanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onLog(selected) }) { Text("Log") }
        }
    }
}

@Composable
private fun SpeciesRow(species: Species) {
    RaisedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(Color(species.accent), CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(species.commonName, fontWeight = FontWeight.Bold)
                Text("${species.scientificName} · ${species.size}", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
                Text(species.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SightingRow(item: SightingItem, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.speciesName, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.timestamp)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete sighting") }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeciesPreview() {
    RubyDewTheme {
        SpeciesContent(
            state = SpeciesUiState(
                zipInput = "30301",
                savedZip = "30301",
                region = Region.EAST,
                species = SpeciesCatalog.forRegion(Region.EAST),
                sightings = listOf(SightingItem(1, "Ruby-throated Hummingbird", 33.7, -84.4, 0L)),
                error = null,
            ),
            onZipChange = {},
            onLookUp = {},
            onLogSighting = {},
            onDeleteSighting = {},
            map = { modifier ->
                Box(modifier.background(Color(0xFFCFE8D5)), contentAlignment = Alignment.Center) { Text("Map") }
            },
        )
    }
}
