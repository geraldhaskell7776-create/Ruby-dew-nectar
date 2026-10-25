package com.rubydew.nectar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rubydew.nectar.RubyDewApp
import com.rubydew.nectar.data.FunFacts
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.data.ZipLookup
import com.rubydew.nectar.domain.FunFact
import com.rubydew.nectar.domain.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val regionName: String?,
    val birdOfTheDay: Species,
    val funFact: FunFact,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as RubyDewApp).store
    private val factOffset = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(store.zip, factOffset, ::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(store.zip.value, 0))

    fun nextFact() {
        factOffset.value += 1
    }

    private fun buildState(zip: String, offset: Int): HomeUiState {
        val day = LocalDate.now().dayOfYear
        val region = ZipLookup.regionFor(zip)
        // Local birds when we know the region (Hawaii has none, so fall back to all).
        val local = region?.let(SpeciesCatalog::forRegion)?.takeIf { it.isNotEmpty() } ?: SpeciesCatalog.all
        // The trading card shows photos, so prefer birds that have them: local ones first, then any.
        val withPhotos = SpeciesCatalog.all.filter { it.photos.isNotEmpty() }
        val pool = local.filter { it.photos.isNotEmpty() }.ifEmpty { withPhotos }.ifEmpty { local }
        return HomeUiState(
            regionName = region?.displayName,
            birdOfTheDay = pool[day % pool.size],
            funFact = FunFacts.all[(day + offset) % FunFacts.all.size],
        )
    }
}
