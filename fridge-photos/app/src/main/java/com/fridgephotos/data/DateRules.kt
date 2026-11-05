package com.fridgephotos.data

import java.time.MonthDay

object DateRules {
    /** True when [day] falls in start..end, including ranges that wrap past Dec 31. */
    fun inRange(day: MonthDay, start: MonthDay, end: MonthDay): Boolean =
        if (start <= end) day in start..end else day >= start || day <= end

    /** Number of days the range covers (Dec 16 – Jan 2 = 18). */
    fun spanDays(start: MonthDay, end: MonthDay): Int {
        val s = start.atYear(LEAP_YEAR).dayOfYear
        val e = end.atYear(LEAP_YEAR).dayOfYear
        return (e - s + DAYS_IN_LEAP_YEAR) % DAYS_IN_LEAP_YEAR + 1
    }

    /**
     * The album to show on [today]: the album with photos whose dates cover today.
     * If several overlap, the shortest range wins (a "Christmas" album beats "Winter").
     * If none cover today, the first album with no dates is used.
     */
    fun activeAlbum(albums: List<Album>, today: MonthDay): Album? {
        val withPhotos = albums.filter { it.photos.isNotEmpty() }
        return withPhotos
            .filter { it.hasDates && inRange(today, it.start!!, it.end!!) }
            .minByOrNull { spanDays(it.start!!, it.end!!) }
            ?: withPhotos.firstOrNull { !it.hasDates }
    }

    private const val LEAP_YEAR = 2024
    private const val DAYS_IN_LEAP_YEAR = 366
}
