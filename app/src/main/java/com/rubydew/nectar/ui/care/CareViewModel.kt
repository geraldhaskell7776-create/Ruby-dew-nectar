package com.rubydew.nectar.ui.care

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rubydew.nectar.RubyDewApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CareUiState(
    val lastCleaned: LocalDate?,
    val daysSinceCleaned: Long?,
    val intervalDays: Int,
    val waterCups: Double,
    val fridgeMade: LocalDate? = null,
    val fridgeDaysOld: Long? = null,
) {
    /** True when the feeder has never been logged, or the cleaning interval has been reached. */
    val needsCleaning: Boolean get() = daysSinceCleaned == null || daysSinceCleaned >= intervalDays
    val nextDue: LocalDate? get() = lastCleaned?.plusDays(intervalDays.toLong())
    /** Standard nectar: 4 parts water to 1 part refined white sugar. */
    val sugarCups: Double get() = waterCups / 4.0
    /** Day the fridge nectar should be thrown out (1 week after it was made). */
    val fridgeTossBy: LocalDate? get() = fridgeMade?.plusDays(FRIDGE_LIFE_DAYS)
    val fridgeDaysLeft: Long? get() = fridgeDaysOld?.let { FRIDGE_LIFE_DAYS - it }
    val fridgeExpired: Boolean get() = (fridgeDaysLeft ?: 1) <= 0

    companion object {
        const val FRIDGE_LIFE_DAYS = 7L
    }
}

class CareViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as RubyDewApp).store
    private val waterCups = MutableStateFlow(1.0)

    val uiState: StateFlow<CareUiState> =
        combine(store.lastCleaned, store.cleaningIntervalDays, waterCups, store.fridgeBatch, ::buildState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildState(
                    store.lastCleaned.value,
                    store.cleaningIntervalDays.value,
                    waterCups.value,
                    store.fridgeBatch.value,
                ),
            )

    fun logCleaning() = store.logCleaning()

    fun logFridgeBatch() = store.logFridgeBatch()

    fun clearFridgeBatch() = store.clearFridgeBatch()

    fun changeWater(deltaCups: Double) {
        waterCups.value = (waterCups.value + deltaCups).coerceIn(0.5, 16.0)
    }

    private fun buildState(last: LocalDate?, interval: Int, water: Double, fridge: LocalDate?) = CareUiState(
        lastCleaned = last,
        daysSinceCleaned = last?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) },
        intervalDays = interval,
        waterCups = water,
        fridgeMade = fridge,
        fridgeDaysOld = fridge?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) },
    )
}
