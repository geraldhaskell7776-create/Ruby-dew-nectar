package com.rubydew.nectar.ui.home

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import com.rubydew.nectar.RubyDewApp
import com.rubydew.nectar.data.FunFacts
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.data.ZipLookup
import com.rubydew.nectar.domain.FunFact
import com.rubydew.nectar.domain.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One baseball-style card: a bird, one of its pictures, and the card's two border colors. */
data class BirdCardPick(
    val species: Species,
    @DrawableRes val photo: Int,
    val colors: CardColors,
)

data class CardColors(val top: Long, val bottom: Long, val nameBar: Long)

data class HomeUiState(
    val regionName: String?,
    val card: BirdCardPick?,
    val funFact: FunFact,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as RubyDewApp).store

    private val _uiState = MutableStateFlow(HomeUiState(null, null, FunFacts.all.random()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        shuffle()
    }

    /** Picks a new random card and fun fact. Called every time the Home screen is shown. */
    fun shuffle() {
        val previous = _uiState.value
        val zip = store.zip.value
        val region = ZipLookup.regionFor(zip)

        // Every (bird, picture) pair, from all birds regardless of ZIP code.
        val pool = SpeciesCatalog.all.flatMap { s -> s.photos.map { s to it } }

        val (species, photo) = pool.randomDifferentFrom(previous.card?.let { it.species to it.photo })
            ?: (null to 0)

        _uiState.value = HomeUiState(
            regionName = region?.displayName,
            card = species?.let { BirdCardPick(it, photo, CARD_COLORS.random()) },
            funFact = FunFacts.all.randomDifferentFrom(previous.funFact) ?: previous.funFact,
        )
    }

    private fun <T> List<T>.randomDifferentFrom(last: T?): T? =
        filter { it != last }.ifEmpty { this }.randomOrNull()

    companion object {
        /** Two-tone border color pairs in ruby tones, inspired by 1970s baseball cards. */
        val CARD_COLORS = listOf(
            CardColors(top = 0xFF6A0B14, bottom = 0xFFC2185B, nameBar = 0xFF8E0E2E), // garnet / rose
            CardColors(top = 0xFF4A0A12, bottom = 0xFFB71C1C, nameBar = 0xFF7F0000), // deep ruby / crimson
            CardColors(top = 0xFF5C0A2E, bottom = 0xFFD32F2F, nameBar = 0xFF9A0007), // burgundy / red
            CardColors(top = 0xFF3B0710, bottom = 0xFFAD1457, nameBar = 0xFF78002E), // black cherry / raspberry
            CardColors(top = 0xFF7A0E19, bottom = 0xFFB8860B, nameBar = 0xFF8B6508), // wine / antique gold
        )
    }
}
