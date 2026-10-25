package com.rubydew.nectar.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rubydew.nectar.data.FunFacts
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.domain.FunFact
import com.rubydew.nectar.domain.Species
import com.rubydew.nectar.ui.theme.CardGold
import com.rubydew.nectar.ui.theme.Leaf
import com.rubydew.nectar.ui.theme.Neutral
import com.rubydew.nectar.ui.theme.RubyDewTheme

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeContent(
        state = state,
        onFactClick = { openUrl(context, it.url) },
        onNextFact = viewModel::nextFact,
    )
}

@Composable
fun HomeContent(state: HomeUiState, onFactClick: (FunFact) -> Unit, onNextFact: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RegionStatusIndicator(state.regionName)
            Spacer(Modifier.weight(1f))
            Text(
                "Ruby Dew Nectar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Text("Featured bird", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        BirdCard(state.birdOfTheDay)

        FunFactBanner(fact = state.funFact, onClick = { onFactClick(state.funFact) }, onNext = onNextFact)
    }
}

/** Top-left dot: green when the app knows your region (from your ZIP), gray when it doesn't. */
@Composable
fun RegionStatusIndicator(regionName: String?) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (regionName != null) Leaf else Neutral, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                regionName ?: "Set ZIP for local birds",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Trading-card style hero: gold frame, swipeable photo gallery, name banner and stats. */
@Composable
fun BirdCard(species: Species) {
    val accent = Color(species.accent)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(4.dp, CardGold),
        colors = CardDefaults.cardColors(containerColor = accent, contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Name banner, like the top of a trading card
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(species.commonName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(species.scientificName, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
                }
                Text(species.size, style = MaterialTheme.typography.labelLarge, color = CardGold)
            }

            if (species.photos.isNotEmpty()) {
                PhotoGallery(species)
            } else {
                InitialsBadge(species.commonName)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.25f),
                contentColor = Color.White,
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(species.description)
                    Text(
                        "Range: " + species.regions.joinToString { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("★ ${species.highlight}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGallery(species: Species) {
    val pagerState = rememberPagerState(pageCount = { species.photos.size })
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, CardGold, RoundedCornerShape(12.dp)),
        ) { page ->
            Image(
                painter = painterResource(species.photos[page]),
                contentDescription = "${species.commonName}, picture ${page + 1} of ${species.photos.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (species.photos.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(species.photos.size) { i ->
                    Box(
                        Modifier
                            .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                            .background(
                                if (i == pagerState.currentPage) CardGold else Color.White.copy(alpha = 0.5f),
                                CircleShape,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialsBadge(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.split(" ", "-").take(2).joinToString("") { it.take(1) },
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun FunFactBanner(fact: FunFact, onClick: () -> Unit, onNext: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Did you know? · ${fact.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(fact.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tap to read more →", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onNext) { Text("Next fact") }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found to open the link", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    RubyDewTheme {
        HomeContent(
            state = HomeUiState(
                regionName = "Eastern U.S.",
                birdOfTheDay = SpeciesCatalog.byId("black_chinned")!!,
                funFact = FunFacts.all[1],
            ),
            onFactClick = {},
            onNextFact = {},
        )
    }
}
