package com.rubydew.nectar.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rubydew.nectar.data.FunFacts
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.domain.FunFact
import com.rubydew.nectar.ui.theme.CardGold
import com.rubydew.nectar.ui.theme.ExtrudedText
import com.rubydew.nectar.ui.theme.Garnet
import com.rubydew.nectar.ui.theme.RaisedElevation
import com.rubydew.nectar.ui.theme.Leaf
import com.rubydew.nectar.ui.theme.Ruby
import com.rubydew.nectar.ui.theme.RubyDark
import com.rubydew.nectar.ui.theme.Neutral
import com.rubydew.nectar.ui.theme.RaisedCard
import com.rubydew.nectar.ui.theme.RubyDewTheme

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // New random card and fun fact each time Home comes up.
    LaunchedEffect(Unit) { viewModel.shuffle() }
    HomeContent(
        state = state,
        onFactClick = { openUrl(context, it.url) },
    )
}

@Composable
fun HomeContent(state: HomeUiState, onFactClick: (FunFact) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderPlaque(regionName = state.regionName)

        state.card?.let {
            BaseballCard(
                pick = it,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            )
        }

        FunFactBanner(fact = state.funFact, onClick = { onFactClick(state.funFact) })
    }
}

/** Raised ruby plaque across the top: gold rim, bevel shading and a drop shadow. */
@Composable
fun HeaderPlaque(regionName: String?) {
    RaisedCard(
        modifier = Modifier.fillMaxWidth(),
        contentColor = Color.White,
        background = Brush.verticalGradient(listOf(Color(0xFFB3172B), Ruby, Garnet, RubyDark)),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RegionStatusIndicator(regionName)
                Spacer(Modifier.weight(1f))
                ExtrudedText(
                    "Ruby Dew Nectar",
                    fontSize = 20.sp,
                    face = CardGold,
                    faceHighlight = Color(0xFFFFF4C9),
                    depth = 3.dp,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

/** Status pill: gold dot when the app knows your region (from your ZIP), gray when it doesn't. */
@Composable
fun RegionStatusIndicator(regionName: String?) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.3f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .shadow(4.dp, CircleShape)
                    .background(if (regionName != null) Leaf else Neutral, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                regionName ?: "Set ZIP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val CardYellow = Color(0xFFFFE135)
private val CardInk = Color(0xFF1A1A1A)

/** Shift the crop left of center so the whole bird (wing tip to beak) stays in frame. */
private val PhotoCropAlignment = BiasAlignment(horizontalBias = -0.45f, verticalBias = 0f)

/** A 1970s-style baseball card: two-tone border, team name on top, photo, name bar and a round badge. */
@Composable
fun BaseballCard(pick: BirdCardPick, modifier: Modifier = Modifier) {
    val top = Color(pick.colors.top)
    val bottom = Color(pick.colors.bottom)
    val outlined = TextStyle(shadow = Shadow(color = CardInk, offset = Offset(4f, 4f), blurRadius = 0f))

    // White outer edge, like card stock
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = RaisedElevation,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .drawBehind {
                    drawRect(top, size = Size(size.width, size.height * 0.55f))
                    drawRect(bottom, topLeft = Offset(0f, size.height * 0.55f))
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExtrudedText(
                "RUBY DEW",
                fontSize = 34.sp,
                face = CardYellow,
                faceHighlight = Color(0xFFFFFBE0),
                depthColor = Color(0xFF2B0A0E),
                depth = 5.dp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))

            Box {
                Image(
                    painter = painterResource(pick.photo),
                    contentDescription = pick.species.commonName,
                    contentScale = ContentScale.Crop,
                    alignment = PhotoCropAlignment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.95f)
                        .border(5.dp, Color.White, RoundedCornerShape(18.dp))
                        .padding(5.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                // Round badge in the corner, like the position ball
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 22.dp)
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .border(3.dp, Color(pick.colors.nameBar), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Humming-\nbird",
                        color = Color(0xFF9B111E),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                pick.species.commonName.removeSuffix(" Hummingbird").uppercase(),
                style = outlined.copy(shadow = Shadow(CardInk, Offset(3f, 3f), 0f)),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = CardYellow,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 64.dp),
            )
        }
    }
}

@Composable
fun FunFactBanner(fact: FunFact, onClick: () -> Unit) {
    RaisedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Did you know? · ${fact.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(fact.text)
            Text("Tap to read more →", style = MaterialTheme.typography.labelLarge)
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
        val bird = SpeciesCatalog.byId("black_chinned")!!
        HomeContent(
            state = HomeUiState(
                regionName = "Desert Southwest",
                card = BirdCardPick(bird, bird.photos.first(), HomeViewModel.CARD_COLORS.first()),
                funFact = FunFacts.all[1],
            ),
            onFactClick = {},
        )
    }
}
