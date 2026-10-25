package com.rubydew.nectar.data

import com.rubydew.nectar.R
import com.rubydew.nectar.domain.FunFact
import com.rubydew.nectar.domain.Region
import com.rubydew.nectar.domain.Region.*
import com.rubydew.nectar.domain.Species

/** Built-in species list. Works offline, no account or fees needed. */
object SpeciesCatalog {
    val all: List<Species> = listOf(
        Species(
            id = "ruby_throated",
            commonName = "Ruby-throated Hummingbird",
            scientificName = "Archilochus colubris",
            size = "3–3.5 in",
            description = "The only hummingbird that regularly nests in the eastern U.S. Males flash a glowing ruby-red throat.",
            highlight = "Many fly about 500 miles nonstop across the Gulf of Mexico during migration.",
            regions = setOf(EAST, PLAINS, TEXAS),
            accent = 0xFF7A0E19,
            photos = listOf(R.drawable.ruby_throated_photo),
        ),
        Species(
            id = "black_chinned",
            commonName = "Black-chinned Hummingbird",
            scientificName = "Archilochus alexandri",
            size = "3.5 in",
            description = "A western cousin of the Ruby-throated. The male's black chin has a thin band of violet below it.",
            highlight = "Often pumps its tail while hovering at flowers and feeders.",
            regions = setOf(SOUTHWEST, TEXAS, ROCKIES, CALIFORNIA, PACIFIC_NW, PLAINS),
            accent = 0xFF311B92,
            photos = listOf(
                R.drawable.black_chinned_photo,
                R.drawable.black_chinned_watercolor_flight,
                R.drawable.black_chinned_watercolor_perched,
            ),
        ),
        Species(
            id = "annas",
            commonName = "Anna's Hummingbird",
            scientificName = "Calypte anna",
            size = "3.9 in",
            description = "A year-round resident along the Pacific coast. Males have an iridescent rose-pink head.",
            highlight = "Males dive from about 100 feet up, making a loud chirp with their tail feathers.",
            regions = setOf(CALIFORNIA, PACIFIC_NW, SOUTHWEST),
            accent = 0xFF880E4F,
            photos = listOf(R.drawable.annas_photo),
        ),
        Species(
            id = "rufous",
            commonName = "Rufous Hummingbird",
            scientificName = "Selasphorus rufus",
            size = "2.8–3.5 in",
            description = "A fiery orange hummingbird known for chasing other birds away from feeders.",
            highlight = "Has one of the longest migrations of any bird for its body size, up to about 3,900 miles one way.",
            regions = setOf(PACIFIC_NW, ALASKA, ROCKIES, CALIFORNIA, SOUTHWEST),
            accent = 0xFF8D3B0B,
            photos = listOf(R.drawable.rufous_photo),
        ),
        Species(
            id = "allens",
            commonName = "Allen's Hummingbird",
            scientificName = "Selasphorus sasin",
            size = "3–3.5 in",
            description = "A coastal California specialty with a coppery body and green back.",
            highlight = "Males swing back and forth in a pendulum-shaped display flight.",
            regions = setOf(CALIFORNIA),
            accent = 0xFF6D3A00,
        ),
        Species(
            id = "broad_tailed",
            commonName = "Broad-tailed Hummingbird",
            scientificName = "Selasphorus platycercus",
            size = "3.5–4 in",
            description = "A mountain hummingbird of high meadows and forests.",
            highlight = "Males make a metallic trilling whistle with their wings as they fly.",
            regions = setOf(ROCKIES, SOUTHWEST),
            accent = 0xFF1B5E20,
        ),
        Species(
            id = "calliope",
            commonName = "Calliope Hummingbird",
            scientificName = "Selasphorus calliope",
            size = "2.8–3.5 in",
            description = "Males have a striking throat of magenta streaks on white.",
            highlight = "The smallest bird that breeds in the U.S. and Canada.",
            regions = setOf(ROCKIES, PACIFIC_NW),
            accent = 0xFF4A148C,
        ),
        Species(
            id = "costas",
            commonName = "Costa's Hummingbird",
            scientificName = "Calypte costae",
            size = "3–3.5 in",
            description = "A desert hummingbird. Males wear a purple crown with long purple 'mustache' feathers.",
            highlight = "Well adapted to hot, dry desert scrub.",
            regions = setOf(SOUTHWEST, CALIFORNIA),
            accent = 0xFF4527A0,
        ),
        Species(
            id = "broad_billed",
            commonName = "Broad-billed Hummingbird",
            scientificName = "Cynanthus latirostris",
            size = "3.5–4 in",
            description = "Deep green and sapphire blue, with a bright red-orange bill.",
            highlight = "In the U.S., found mostly in canyons of southern Arizona and New Mexico.",
            regions = setOf(SOUTHWEST),
            accent = 0xFF0D47A1,
        ),
        Species(
            id = "rivolis",
            commonName = "Rivoli's Hummingbird",
            scientificName = "Eugenes fulgens",
            size = "4.5–5 in",
            description = "One of the largest hummingbirds in the U.S., with a purple crown and green throat.",
            highlight = "Its slower wingbeats can make it look almost like a large swift.",
            regions = setOf(SOUTHWEST),
            accent = 0xFF004D40,
        ),
        Species(
            id = "buff_bellied",
            commonName = "Buff-bellied Hummingbird",
            scientificName = "Amazilia yucatanensis",
            size = "4–4.5 in",
            description = "A Gulf Coast specialty with a green throat, buffy belly and red bill.",
            highlight = "In the U.S., mainly seen in south Texas.",
            regions = setOf(TEXAS),
            accent = 0xFF33691E,
        ),
    )

    fun byId(id: String): Species? = all.firstOrNull { it.id == id }

    fun forRegion(region: Region): List<Species> = all.filter { region in it.regions }
}

/**
 * Daily fun facts. Each one links to a free educational page.
 * Swap the urls for Ruby Dew Nectar blog posts once they exist.
 */
object FunFacts {
    val all: List<FunFact> = listOf(
        FunFact(
            title = "Flight",
            text = "Hummingbirds can hover, fly backwards and even fly upside down for a moment. Small species beat their wings around 50–80 times a second.",
            url = "https://en.wikipedia.org/wiki/Hummingbird",
        ),
        FunFact(
            title = "Torpor",
            text = "On cold nights, hummingbirds enter torpor, a deep sleep where their body temperature and heart rate drop to save energy.",
            url = "https://en.wikipedia.org/wiki/Torpor",
        ),
        FunFact(
            title = "Built-in GPS",
            text = "Hummingbirds remember which flowers and feeders they've visited, and can return to the same feeder after migrating thousands of miles.",
            url = "https://en.wikipedia.org/wiki/Hummingbird",
        ),
        FunFact(
            title = "Gulf crossing",
            text = "Many Ruby-throated Hummingbirds fly about 500 miles nonstop across the Gulf of Mexico.",
            url = "https://www.allaboutbirds.org/guide/Ruby-throated_Hummingbird",
        ),
        FunFact(
            title = "Epic migration",
            text = "For its size, the Rufous Hummingbird has one of the longest migrations of any bird, up to about 3,900 miles one way.",
            url = "https://www.allaboutbirds.org/guide/Rufous_Hummingbird",
        ),
        FunFact(
            title = "Heartbeat",
            text = "A hummingbird's heart can beat more than 1,000 times per minute while it's flying.",
            url = "https://en.wikipedia.org/wiki/Hummingbird",
        ),
        FunFact(
            title = "Tiny giant",
            text = "The Calliope Hummingbird is the smallest bird that breeds in the U.S. and Canada, about 3 inches long.",
            url = "https://www.allaboutbirds.org/guide/Calliope_Hummingbird",
        ),
        FunFact(
            title = "Bird migration",
            text = "Most North American hummingbirds migrate south for winter, following the flowers and the insects they eat.",
            url = "https://en.wikipedia.org/wiki/Bird_migration",
        ),
    )
}

/** Offline ZIP lookup: the first 3 digits of a U.S. ZIP code map to a state. */
object ZipLookup {
    private val prefixRanges: List<Triple<Int, Int, String>> = listOf(
        Triple(10, 27, "MA"), Triple(28, 29, "RI"), Triple(30, 38, "NH"), Triple(39, 49, "ME"),
        Triple(50, 54, "VT"), Triple(55, 55, "MA"), Triple(56, 59, "VT"), Triple(60, 69, "CT"),
        Triple(70, 89, "NJ"), Triple(100, 149, "NY"), Triple(150, 196, "PA"), Triple(197, 199, "DE"),
        Triple(200, 205, "DC"), Triple(206, 219, "MD"), Triple(220, 246, "VA"), Triple(247, 268, "WV"),
        Triple(270, 289, "NC"), Triple(290, 299, "SC"), Triple(300, 319, "GA"), Triple(320, 349, "FL"),
        Triple(350, 369, "AL"), Triple(370, 385, "TN"), Triple(386, 397, "MS"), Triple(398, 399, "GA"),
        Triple(400, 427, "KY"), Triple(430, 459, "OH"), Triple(460, 479, "IN"), Triple(480, 499, "MI"),
        Triple(500, 528, "IA"), Triple(530, 549, "WI"), Triple(550, 567, "MN"), Triple(570, 577, "SD"),
        Triple(580, 588, "ND"), Triple(590, 599, "MT"), Triple(600, 629, "IL"), Triple(630, 658, "MO"),
        Triple(660, 679, "KS"), Triple(680, 693, "NE"), Triple(700, 714, "LA"), Triple(716, 729, "AR"),
        Triple(730, 749, "OK"), Triple(750, 799, "TX"), Triple(800, 816, "CO"), Triple(820, 831, "WY"),
        Triple(832, 838, "ID"), Triple(840, 847, "UT"), Triple(850, 865, "AZ"), Triple(870, 884, "NM"),
        Triple(885, 885, "TX"), Triple(889, 898, "NV"), Triple(900, 961, "CA"), Triple(967, 968, "HI"),
        Triple(970, 979, "OR"), Triple(980, 994, "WA"), Triple(995, 999, "AK"),
    )

    private val stateRegions: Map<String, Region> = buildMap {
        listOf(
            "MA", "RI", "NH", "ME", "VT", "CT", "NJ", "NY", "PA", "DE", "DC", "MD", "VA", "WV",
            "NC", "SC", "GA", "FL", "AL", "TN", "MS", "KY", "OH", "IN", "MI", "IA", "WI", "MN",
            "IL", "MO", "LA", "AR",
        ).forEach { put(it, EAST) }
        listOf("ND", "SD", "NE", "KS", "OK").forEach { put(it, PLAINS) }
        put("TX", TEXAS)
        listOf("AZ", "NM", "NV").forEach { put(it, SOUTHWEST) }
        listOf("CO", "WY", "MT", "ID", "UT").forEach { put(it, ROCKIES) }
        listOf("WA", "OR").forEach { put(it, PACIFIC_NW) }
        put("CA", CALIFORNIA)
        put("AK", ALASKA)
        put("HI", HAWAII)
    }

    fun isValid(zip: String): Boolean = zip.length == 5 && zip.all { it.isDigit() }

    fun stateFor(zip: String): String? {
        if (!isValid(zip)) return null
        val prefix = zip.take(3).toInt()
        return prefixRanges.firstOrNull { prefix in it.first..it.second }?.third
    }

    fun regionFor(zip: String): Region? = stateFor(zip)?.let { stateRegions[it] }
}
