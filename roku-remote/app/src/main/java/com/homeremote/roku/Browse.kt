package com.homeremote.roku

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** One channel: open it on the TV, or browse its popular shows and movies and star favorites. */
@Composable
fun ChannelPage(vm: RemoteViewModel, app: RokuApp, onBack: () -> Unit, onLaunched: () -> Unit) {
    val view = LocalView.current
    var kind by rememberSaveable(app.id) { mutableStateOf(TV) }
    val state = vm.browse["${app.id}/$kind"] ?: Browse()
    LaunchedEffect(app.id, kind) { if (vm.canBrowse) vm.browse(app, kind) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to channels") }
                vm.icons[app.id]?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(app.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    vm.launch(app)
                    onLaunched()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Open ${app.name} on the TV")
            }
        }

        if (!vm.canBrowse) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Hint("Show browsing is off in this copy of the app: it was built without a TMDB token.")
            }
            return@LazyVerticalGrid
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchField(vm, "Search ${app.name}")
        }

        if (vm.searchQuery.isNotBlank()) {
            val hits = vm.searchHits.filter { hit -> hit.onYourRoku.any { it.id == app.id } }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchStatus(vm, empty = hits.isEmpty(), emptyText = "No matches on ${app.name}. Clear the search to see what else is on the Channels tab.")
            }
            items(hits, key = { "s-${it.title.kind}-${it.title.id}" }) { hit ->
                PosterCard(
                    title = hit.title,
                    starred = vm.isFavorite(app.id, hit.title),
                    onStar = { vm.toggleFavorite(app, hit.title) },
                    onOpen = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        vm.openTitle(app.id, app.name, hit.title)
                        onLaunched()
                    },
                )
            }
            return@LazyVerticalGrid
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == TV, onClick = { kind = TV }, label = { Text("TV shows") })
                FilterChip(selected = kind == MOVIE, onClick = { kind = MOVIE }, label = { Text("Movies") })
            }
        }

        when {
            state.noMatch -> item(span = { GridItemSpan(maxLineSpan) }) {
                Hint("TMDB doesn't list shows for ${app.name}. You can still open it on the TV above.")
            }
            state.titles.isEmpty() && state.error == null && !state.loading && state.page > 0 ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Hint("Nothing listed here for your country yet.")
                }
        }

        items(state.titles, key = { "${it.kind}-${it.id}" }) { title ->
            PosterCard(
                title = title,
                starred = vm.isFavorite(app.id, title),
                onStar = { vm.toggleFavorite(app, title) },
                onOpen = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    vm.openTitle(app.id, app.name, title)
                    onLaunched()
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.browse(app, kind, more = state.page > 0) }) { Text("Try again") }
                }
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                } else if (state.error == null && state.hasMore && state.page > 0 && state.titles.isNotEmpty()) {
                    OutlinedButton(onClick = { vm.browse(app, kind, more = true) }) { Text("Show more") }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tap a poster to open ${app.name} on the TV, then find it there. Tap the star to save it.\n" +
                        "Show lists come from TMDB. This app uses the TMDB API but is not endorsed or certified by TMDB.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Starred shows across all channels, as a row of posters at the top of the Channels tab. */
@Composable
fun FavoritesRow(vm: RemoteViewModel, onLaunched: () -> Unit) {
    val view = LocalView.current
    Column {
        Text("Favorites", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.favorites, key = { "${it.appId}-${it.title.kind}-${it.title.id}" }) { f ->
                Column(Modifier.width(104.dp)) {
                    PosterCard(
                        title = f.title,
                        starred = true,
                        onStar = { vm.removeFavorite(f) },
                        onOpen = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            vm.openTitle(f.appId, f.appName, f.title)
                            onLaunched()
                        },
                    )
                    Text(f.appName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PosterCard(
    title: Title,
    starred: Boolean,
    onStar: () -> Unit,
    onOpen: () -> Unit,
    showStar: Boolean = true,
    showName: Boolean = true,
) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpen),
        ) {
            if (title.poster != null) {
                AsyncImage(
                    model = Tmdb.posterUrl(title.poster),
                    contentDescription = title.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    title.name,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(6.dp),
                )
            }
            if (showStar) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onStar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (starred) "Remove ${title.name} from favorites" else "Add ${title.name} to favorites",
                    tint = if (starred) ChristmasGold else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (!showName) return@Column
        Spacer(Modifier.height(4.dp))
        Text(title.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (title.year.isNotBlank()) {
            Text(title.year, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

/** The box for searching shows and movies by name. */
@Composable
fun SearchField(vm: RemoteViewModel, placeholder: String) {
    androidx.compose.material3.OutlinedTextField(
        value = vm.searchQuery,
        onValueChange = vm::search,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (vm.searchQuery.isNotEmpty()) {
                IconButton(onClick = { vm.search("") }) { Icon(Icons.Filled.Close, "Clear search") }
            }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Searching / error / "nothing found" line under the search box, or null when results are showing. */
@Composable
fun SearchStatus(vm: RemoteViewModel, empty: Boolean, emptyText: String) {
    when {
        vm.searching -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Searching…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        vm.searchError != null -> Text(vm.searchError!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        empty -> Hint(emptyText)
    }
}

/** One search result: poster, name, and buttons for the channels on this Roku that have it. */
@Composable
fun SearchResultRow(vm: RemoteViewModel, hit: SearchHit, onLaunched: () -> Unit) {
    val view = LocalView.current
    val first = hit.onYourRoku.firstOrNull()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
    ) {
        Box(Modifier.width(78.dp)) {
            PosterCard(
                title = hit.title,
                starred = first != null && vm.isFavorite(first.id, hit.title),
                onStar = { first?.let { vm.toggleFavorite(it, hit.title) } },
                onOpen = {
                    if (first != null) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        vm.openTitle(first.id, first.name, hit.title)
                        onLaunched()
                    }
                },
                showStar = first != null,
                showName = false,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(hit.title.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val kind = if (hit.title.kind == TV) "TV show" else "Movie"
            Text(
                listOf(kind, hit.title.year).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (hit.onYourRoku.isEmpty()) {
                Text("Not on any channel on your Roku.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Watch on:", fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hit.onYourRoku, key = { it.id }) { app ->
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                vm.openTitle(app.id, app.name, hit.title)
                                onLaunched()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                        ) { Text(app.name, fontSize = 13.sp, maxLines = 1) }
                    }
                }
            }
            if (hit.elsewhere.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Also on: " + hit.elsewhere.joinToString(", "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
