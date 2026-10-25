package com.rubydew.nectar.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rubydew.nectar.RubyDewApp
import com.rubydew.nectar.data.ZipLookup
import com.rubydew.nectar.domain.PackageType
import com.rubydew.nectar.domain.Shipment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class SettingsUiState(
    val zip: String,
    val regionName: String?,
    val stateCode: String?,
    val intervalDays: Int,
    val shipments: List<Shipment>,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as RubyDewApp).store

    val uiState: StateFlow<SettingsUiState> =
        combine(store.zip, store.cleaningIntervalDays, store.shipments, ::buildState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildState(store.zip.value, store.cleaningIntervalDays.value, store.shipments.value),
            )

    /** Returns an error message, or null when saved. */
    fun saveZip(zip: String): String? {
        if (zip.isEmpty()) {
            store.setZip("")
            return null
        }
        if (!ZipLookup.isValid(zip)) return "Enter a 5-digit ZIP code."
        if (ZipLookup.regionFor(zip) == null) return "We couldn't find that ZIP code."
        store.setZip(zip)
        return null
    }

    fun setInterval(days: Int) = store.setCleaningInterval(days)

    /** Returns an error message, or null when the shipment was added. */
    fun addShipment(type: PackageType, tracking: String, dateText: String): String? {
        val date = try {
            LocalDate.parse(dateText.trim())
        } catch (e: DateTimeParseException) {
            return "Enter the date as YYYY-MM-DD, for example 2026-11-05."
        }
        store.addShipment(Shipment(store.newId(), type, tracking.trim(), date))
        return null
    }

    fun deleteShipment(id: Long) = store.deleteShipment(id)

    private fun buildState(zip: String, interval: Int, shipments: List<Shipment>) = SettingsUiState(
        zip = zip,
        regionName = ZipLookup.regionFor(zip)?.displayName,
        stateCode = ZipLookup.stateFor(zip),
        intervalDays = interval,
        shipments = shipments.sortedBy { it.scheduledDate },
    )
}
