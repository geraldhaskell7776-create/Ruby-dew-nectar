package com.rubydew.nectar.data

import android.content.Context
import com.rubydew.nectar.domain.PackageType
import com.rubydew.nectar.domain.Shipment
import com.rubydew.nectar.domain.Sighting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Everything the app saves lives here, on the phone only (SharedPreferences).
 * No accounts, servers or fees. No photos are stored.
 */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("ruby_dew_nectar", Context.MODE_PRIVATE)

    private val _zip = MutableStateFlow(prefs.getString(KEY_ZIP, "") ?: "")
    val zip: StateFlow<String> = _zip.asStateFlow()

    private val _lastCleaned = MutableStateFlow(
        prefs.getLong(KEY_LAST_CLEANED, NONE).takeIf { it != NONE }?.let(LocalDate::ofEpochDay)
    )
    val lastCleaned: StateFlow<LocalDate?> = _lastCleaned.asStateFlow()

    private val _cleaningInterval = MutableStateFlow(prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL_DAYS))
    val cleaningIntervalDays: StateFlow<Int> = _cleaningInterval.asStateFlow()

    private val _sightings = MutableStateFlow(loadSightings())
    val sightings: StateFlow<List<Sighting>> = _sightings.asStateFlow()

    private val _shipments = MutableStateFlow(loadShipments())
    val shipments: StateFlow<List<Shipment>> = _shipments.asStateFlow()

    fun setZip(value: String) {
        prefs.edit().putString(KEY_ZIP, value).apply()
        _zip.value = value
    }

    fun logCleaning(date: LocalDate = LocalDate.now()) {
        prefs.edit().putLong(KEY_LAST_CLEANED, date.toEpochDay()).apply()
        _lastCleaned.value = date
    }

    fun setCleaningInterval(days: Int) {
        prefs.edit().putInt(KEY_INTERVAL, days).apply()
        _cleaningInterval.value = days
    }

    fun addSighting(sighting: Sighting) = saveSightings(_sightings.value + sighting)

    fun deleteSighting(id: Long) = saveSightings(_sightings.value.filterNot { it.id == id })

    fun addShipment(shipment: Shipment) = saveShipments(_shipments.value + shipment)

    fun deleteShipment(id: Long) = saveShipments(_shipments.value.filterNot { it.id == id })

    fun newId(): Long = System.nanoTime()

    private fun saveSightings(list: List<Sighting>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("speciesId", it.speciesId)
                    .put("lat", it.latitude)
                    .put("lon", it.longitude)
                    .put("time", it.timestamp)
            )
        }
        prefs.edit().putString(KEY_SIGHTINGS, arr.toString()).apply()
        _sightings.value = list
    }

    private fun loadSightings(): List<Sighting> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_SIGHTINGS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Sighting(
                id = o.getLong("id"),
                speciesId = o.getString("speciesId"),
                latitude = o.getDouble("lat"),
                longitude = o.getDouble("lon"),
                timestamp = o.getLong("time"),
            )
        }
    }.getOrDefault(emptyList())

    private fun saveShipments(list: List<Shipment>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("type", it.packageType.name)
                    .put("tracking", it.trackingNumber)
                    .put("date", it.scheduledDate.toEpochDay())
            )
        }
        prefs.edit().putString(KEY_SHIPMENTS, arr.toString()).apply()
        _shipments.value = list
    }

    private fun loadShipments(): List<Shipment> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_SHIPMENTS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Shipment(
                id = o.getLong("id"),
                packageType = PackageType.valueOf(o.getString("type")),
                trackingNumber = o.getString("tracking"),
                scheduledDate = LocalDate.ofEpochDay(o.getLong("date")),
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val DEFAULT_INTERVAL_DAYS = 3
        private const val NONE = Long.MIN_VALUE
        private const val KEY_ZIP = "zip"
        private const val KEY_LAST_CLEANED = "last_cleaned_epoch_day"
        private const val KEY_INTERVAL = "cleaning_interval_days"
        private const val KEY_SIGHTINGS = "sightings"
        private const val KEY_SHIPMENTS = "shipments"
    }
}
