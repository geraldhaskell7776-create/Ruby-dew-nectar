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

        // Every (bird, picture) pair. Prefer birds found near the user's ZIP, if any have pictures.
        val allPicks = SpeciesCatalog.all.flatMap { s -> s.photos.map { s to it } }
        val localPicks = region?.let { r -> allPicks.filter { r in it.first.regions } }.orEmpty()
        val pool = localPicks.ifEmpty { allPicks }

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
        /** Two-tone border color pairs, inspired by 1970s baseball cards. */
        val CARD_COLORS = listOf(
            CardColors(top = 0xFF5B2A86, bottom = 0xFFE0447F, nameBar = 0xFFB0245E), // purple / pink
            CardColors(top = 0xFF9B111E, bottom = 0xFFF2A541, nameBar = 0xFFC9771B), // ruby / gold
            CardColors(top = 0xFF1B5E20, bottom = 0xFF4FC3F7, nameBar = 0xFF0277BD), // green / sky
            CardColors(top = 0xFF0D47A1, bottom = 0xFFFF7043, nameBar = 0xFFD84315), // blue / orange
            CardColors(top = 0xFF006064, bottom = 0xFFFFD54F, nameBar = 0xFFE6A100), // teal / yellow
        )
    }
}
