package com.rubydew.nectar.ui.species

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rubydew.nectar.RubyDewApp
import com.rubydew.nectar.data.SpeciesCatalog
import com.rubydew.nectar.data.ZipLookup
import com.rubydew.nectar.domain.Region
import com.rubydew.nectar.domain.Sighting
import com.rubydew.nectar.domain.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SightingItem(
    val id: Long,
    val speciesName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)

data class SpeciesUiState(
    val zipInput: String,
    val savedZip: String,
    val region: Region?,
    val species: List<Species>,
    val sightings: List<SightingItem>,
    val error: String?,
) {
    /** Species offered when logging a sighting: local birds, or every bird if none are local. */
    val loggableSpecies: List<Species> get() = species.ifEmpty { SpeciesCatalog.all }
}

class SpeciesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as RubyDewApp).store
    private val zipInput = MutableStateFlow(store.zip.value)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SpeciesUiState> =
        combine(zipInput, store.zip, store.sightings, error, ::buildState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildState(zipInput.value, store.zip.value, store.sightings.value, null),
            )

    fun onZipChange(value: String) {
        zipInput.value = value.filter { it.isDigit() }.take(5)
        error.value = null
    }

    fun lookUp() {
        val zip = zipInput.value
        when {
            !ZipLookup.isValid(zip) -> error.value = "Enter a 5-digit ZIP code."
            ZipLookup.regionFor(zip) == null -> error.value = "We couldn't find that ZIP code."
            else -> {
                error.value = null
                store.setZip(zip)
            }
        }
    }

    fun addSighting(speciesId: String, latitude: Double, longitude: Double) {
        store.addSighting(
            Sighting(
                id = store.newId(),
                speciesId = speciesId,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    fun deleteSighting(id: Long) = store.deleteSighting(id)

    private fun buildState(input: String, saved: String, sightings: List<Sighting>, err: String?): SpeciesUiState {
        val region = ZipLookup.regionFor(saved)
        return SpeciesUiState(
            zipInput = input,
            savedZip = saved,
            region = region,
            species = region?.let(SpeciesCatalog::forRegion) ?: SpeciesCatalog.all,
            sightings = sightings
                .sortedByDescending { it.timestamp }
                .map {
                    SightingItem(
                        id = it.id,
                        speciesName = SpeciesCatalog.byId(it.speciesId)?.commonName ?: "Unknown hummingbird",
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = it.timestamp,
                    )
                },
            error = err,
        )
    }
}
