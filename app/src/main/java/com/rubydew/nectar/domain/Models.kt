package com.rubydew.nectar.domain

import java.time.LocalDate

/** Broad hummingbird regions of the U.S., with a map center for each. */
enum class Region(val displayName: String, val centerLat: Double, val centerLon: Double) {
    EAST("Eastern U.S.", 37.5, -82.0),
    PLAINS("Great Plains", 41.0, -99.0),
    TEXAS("Texas", 31.0, -99.0),
    SOUTHWEST("Desert Southwest", 34.0, -111.5),
    ROCKIES("Rocky Mountains", 42.5, -109.0),
    PACIFIC_NW("Pacific Northwest", 45.5, -121.0),
    CALIFORNIA("California", 36.8, -119.5),
    ALASKA("Alaska", 59.0, -137.0),
    HAWAII("Hawaii", 20.8, -156.3),
}

data class Species(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val size: String,
    val description: String,
    val highlight: String,
    val regions: Set<Region>,
    /** Dark card color (ARGB) so white text stays high-contrast. */
    val accent: Long,
    /** Drawable resource ids for the trading card gallery. Bundled with the app, nothing downloaded or saved. */
    val photos: List<Int> = emptyList(),
)

data class FunFact(
    val title: String,
    val text: String,
    val url: String,
)

data class Sighting(
    val id: Long,
    val speciesId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)

enum class PackageType(val label: String) {
    NECTAR("Nectar"),
    BIRDSEED("Birdseed"),
    FEEDER_PARTS("Feeder parts"),
}

data class Shipment(
    val id: Long,
    val packageType: PackageType,
    val trackingNumber: String,
    val scheduledDate: LocalDate,
)
