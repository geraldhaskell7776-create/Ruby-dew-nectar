package com.fridgephotos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fridgephotos.data.Album
import com.fridgephotos.data.DateRules
import com.fridgephotos.server.SlideshowService
import java.time.Month
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val rangeFormat = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddPhotos: (String) -> Unit,
    onStartSlideshow: () -> Unit,
    onStopSlideshow: () -> Unit,
    onShowHere: () -> Unit,
    onAllowBackground: () -> Unit,
    needsBackgroundPermission: () -> Boolean,
) {
    val albums by viewModel.store.albums.collectAsStateWithLifecycle()
    val seconds by viewModel.store.slideSeconds.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val running by SlideshowService.running.collectAsStateWithLifecycle()
    val serverError by SlideshowService.error.collectAsStateWithLifecycle()

    var address by remember { mutableStateOf(SlideshowService.address()) }
    var askBackground by remember { mutableStateOf(needsBackgroundPermission()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        address = SlideshowService.address()
        askBackground = needsBackgroundPermission()
    }

    var editing by remember { mutableStateOf<Album?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Album?>(null) }

    val today = MonthDay.now()
    val showingNow = DateRules.activeAlbum(albums, today)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Fridge Photos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            item {
                FridgeCard(
                    running = running,
                    address = address,
                    showingNow = showingNow,
                    seconds = seconds,
                    error = serverError,
                    askBackground = askBackground,
                    onToggle = { if (running) onStopSlideshow() else onStartSlideshow() },
                    onChangeSeconds = viewModel::changeSeconds,
                    onShowHere = onShowHere,
                    onAllowBackground = onAllowBackground,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Albums",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { creating = true }) { Text("New album") }
                }
                Text(
                    "Each album plays on its dates every year. If dates overlap, the shorter one wins. " +
                        "An album with no dates plays on all other days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    isShowing = album.id == showingNow?.id,
                    status = status[album.id],
                    onAddPhotos = { onAddPhotos(album.id) },
                    onEdit = { editing = album },
                    onDelete = { deleting = album },
                )
            }
        }
    }

    if (creating || editing != null) {
        AlbumDialog(
            album = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, start, end ->
                viewModel.saveAlbum(editing?.id, name, start, end)
                creating = false
                editing = null
            },
            onClearPhotos = editing?.let { a -> { viewModel.clearPhotos(a.id); editing = null } },
        )
    }

    deleting?.let { album ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${album.name}\"?") },
            text = { Text("This removes the album and its ${album.photos.size} saved copies from this phone. Your Google Photos aren't touched.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAlbum(album.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FridgeCard(
    running: Boolean,
    address: String?,
    showingNow: Album?,
    seconds: Int,
    error: String?,
    askBackground: Boolean,
    onToggle: () -> Unit,
    onChangeSeconds: (Int) -> Unit,
    onShowHere: () -> Unit,
    onAllowBackground: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (running) "Slideshow is on" else "Slideshow is off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = running, onCheckedChange = { onToggle() })
            }
            if (running) {
                Text("On the fridge, open the Browser app and go to:")
                SelectionContainer {
                    Text(
                        address ?: "Connect this phone to your home Wi-Fi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "Keep this phone on the same Wi-Fi and plugged in.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Showing today: " + (showingNow?.let { "${it.name} (${it.photos.size} photos)" } ?: "nothing yet"),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Each photo for $seconds sec", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onChangeSeconds(-5) }) { Text("−") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onChangeSeconds(5) }) { Text("+") }
            }
            OutlinedButton(onClick = onShowHere, modifier = Modifier.fillMaxWidth()) {
                Text("Show on this screen")
            }
            if (askBackground) {
                TextButton(onClick = onAllowBackground) { Text("Let it keep running in the background") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    isShowing: Boolean,
    status: String?,
    onAddPhotos: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (isShowing) {
                    Text("● Showing now", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
            Text(rangeText(album) + " · ${album.photos.size} photos")
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddPhotos) { Text("Add photos from Google") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AlbumDialog(
    album: Album?,
    onDismiss: () -> Unit,
    onSave: (String, MonthDay?, MonthDay?) -> Unit,
    onClearPhotos: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(album?.name ?: "") }
    var useDates by remember { mutableStateOf(album?.hasDates ?: true) }
    var start by remember { mutableStateOf(album?.start ?: MonthDay.of(12, 16)) }
    var end by remember { mutableStateOf(album?.end ?: MonthDay.of(1, 2)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (album == null) "New album" else "Edit album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album name") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useDates, onCheckedChange = { useDates = it })
                    Text("Only show between these dates")
                }
                if (useDates) {
                    MonthDayField("From", start) { start = it }
                    MonthDayField("To", end) { end = it }
                    Text(
                        "Repeats every year. A range like Dec 16 – Jan 2 crosses New Year's.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text("Shows on any day no other album covers.", style = MaterialTheme.typography.bodySmall)
                }
                val photoCount = album?.photos?.size ?: 0
                if (onClearPhotos != null && photoCount > 0) {
                    TextButton(onClick = onClearPhotos) { Text("Remove all $photoCount photos") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), start.takeIf { useDates }, end.takeIf { useDates }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MonthDayField(label: String, value: MonthDay, onChange: (MonthDay) -> Unit) {
    val months = Month.entries
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(44.dp))
        Picker(
            options = months.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
            selected = value.monthValue - 1,
        ) { i ->
            val month = months[i]
            onChange(MonthDay.of(month, value.dayOfMonth.coerceAtMost(month.maxLength())))
        }
        Picker(
            options = (1..value.month.maxLength()).map { it.toString() },
            selected = value.dayOfMonth - 1,
        ) { i -> onChange(MonthDay.of(value.month, i + 1)) }
    }
}

@Composable
private fun Picker(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(options[selected]) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}

private fun rangeText(album: Album): String =
    if (album.hasDates) "${album.start!!.format(rangeFormat)} – ${album.end!!.format(rangeFormat)}"
    else "Any other day"
