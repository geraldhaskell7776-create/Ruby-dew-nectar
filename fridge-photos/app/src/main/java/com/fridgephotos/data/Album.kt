package com.fridgephotos.data

import java.time.MonthDay

/** One saved photo. [file] is the file name inside the album's folder on the phone. */
data class Photo(
    val googleId: String,
    val file: String,
    val takenAt: Long?,
)

/**
 * A folder of photos that shows on the fridge between [start] and [end] every year.
 * The range can wrap past New Year's Eve (Dec 16 – Jan 2).
 * With no dates, the album is the fallback for days no other album covers.
 */
data class Album(
    val id: String,
    val name: String,
    val start: MonthDay?,
    val end: MonthDay?,
    val photos: List<Photo> = emptyList(),
) {
    val hasDates: Boolean get() = start != null && end != null
}
