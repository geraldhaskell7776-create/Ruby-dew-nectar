package com.rubydew.nectar.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rubydew.nectar.domain.PackageType
import com.rubydew.nectar.domain.Shipment
import com.rubydew.nectar.ui.theme.RaisedCard
import com.rubydew.nectar.ui.theme.RubyDewTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val intervalOptions = listOf(2, 3, 5, 7)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onSaveZip = viewModel::saveZip,
        onIntervalChange = viewModel::setInterval,
        onAddShipment = viewModel::addShipment,
        onDeleteShipment = viewModel::deleteShipment,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onSaveZip: (String) -> String?,
    onIntervalChange: (Int) -> Unit,
    onAddShipment: (PackageType, String, String) -> String?,
    onDeleteShipment: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Account & deliveries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SectionCard("Your profile") {
            ZipEditor(state = state, onSave = onSaveZip)
        }

        SectionCard("Cleaning reminder") {
            Text("Clean the feeder every:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                intervalOptions.forEach { days ->
                    FilterChip(
                        selected = state.intervalDays == days,
                        onClick = { onIntervalChange(days) },
                        label = { Text("$days days") },
                    )
                }
            }
            Text(
                "Tip: every 2–3 days in hot weather, weekly when it's cool.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        RaisedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Membership: Free · everything unlocked", fontWeight = FontWeight.Bold)
                    Text("No subscription and no fees. Your data stays on this phone.")
                }
            }
        }

        SectionCard("Upcoming shipments") {
            if (state.shipments.isEmpty()) {
                Text("No shipments yet. Add your nectar and birdseed deliveries below.")
            }
            state.shipments.forEachIndexed { i, shipment ->
                if (i > 0) HorizontalDivider()
                ShipmentRow(shipment, onDelete = { onDeleteShipment(shipment.id) })
            }
        }

        SectionCard("Add a shipment") {
            AddShipmentForm(onAdd = onAddShipment)
        }
    }
}

@Composable
private fun ZipEditor(state: SettingsUiState, onSave: (String) -> String?) {
    var zip by rememberSaveable { mutableStateOf(state.zip) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    // Keep the field in sync if the ZIP is changed on the Species screen.
    LaunchedEffect(state.zip) { zip = state.zip }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = zip,
            onValueChange = { zip = it.filter(Char::isDigit).take(5); message = null },
            label = { Text("ZIP code") },
            singleLine = true,
            isError = isError && message != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val error = onSave(zip)
            isError = error != null
            message = error ?: "Saved"
        }) { Text("Save") }
    }
    message?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary) }
    Text(
        if (state.regionName != null) "Region: ${state.regionName} (${state.stateCode})"
        else "Add your ZIP to see birds and map data for your area.",
    )
}

@Composable
private fun ShipmentRow(shipment: Shipment, onDelete: () -> Unit) {
    val daysAway = ChronoUnit.DAYS.between(LocalDate.now(), shipment.scheduledDate)
    val whenText = when {
        daysAway == 0L -> "Arriving today"
        daysAway == 1L -> "Arriving tomorrow"
        daysAway > 1 -> "In $daysAway days"
        else -> "Scheduled date passed"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(shipment.packageType.label, fontWeight = FontWeight.Bold)
            Text("${shipment.scheduledDate.format(dateFormat)} · $whenText")
            Text(
                if (shipment.trackingNumber.isBlank()) "No tracking number" else "Tracking: ${shipment.trackingNumber}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete shipment") }
    }
}

@Composable
private fun AddShipmentForm(onAdd: (PackageType, String, String) -> String?) {
    var type by rememberSaveable { mutableStateOf(PackageType.NECTAR) }
    var tracking by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Package: ${type.label}")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PackageType.entries.forEach {
                DropdownMenuItem(text = { Text(it.label) }, onClick = { type = it; menuOpen = false })
            }
        }
    }
    OutlinedTextField(
        value = tracking,
        onValueChange = { tracking = it },
        label = { Text("Tracking number (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = date,
        onValueChange = { date = it; error = null },
        label = { Text("Scheduled date (YYYY-MM-DD)") },
        singleLine = true,
        isError = error != null,
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = {
            error = onAdd(type, tracking, date)
            if (error == null) tracking = ""
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Add shipment") }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    RaisedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    RubyDewTheme {
        SettingsContent(
            state = SettingsUiState(
                zip = "30301",
                regionName = "Eastern U.S.",
                stateCode = "GA",
                intervalDays = 3,
                shipments = listOf(
                    Shipment(1, PackageType.NECTAR, "1Z999AA10123456784", LocalDate.now().plusDays(3)),
                    Shipment(2, PackageType.BIRDSEED, "", LocalDate.now().plusDays(10)),
                ),
            ),
            onSaveZip = { null },
            onIntervalChange = {},
            onAddShipment = { _, _, _ -> null },
            onDeleteShipment = {},
        )
    }
}
