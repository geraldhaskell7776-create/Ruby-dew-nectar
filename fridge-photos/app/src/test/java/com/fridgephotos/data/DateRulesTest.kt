package com.fridgephotos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.MonthDay

class DateRulesTest {
    private val photo = listOf(Photo("g1", "a.jpg", null))
    private val newYears = Album("ny", "New Years", MonthDay.of(12, 16), MonthDay.of(1, 2), photo)
    private val christmas = Album("xm", "Christmas", MonthDay.of(12, 24), MonthDay.of(12, 26), photo)
    private val summer = Album("su", "Summer", MonthDay.of(6, 1), MonthDay.of(8, 31), photo)
    private val everyday = Album("ev", "Everyday", null, null, photo)

    @Test
    fun rangeThatWrapsPastNewYear() {
        val s = MonthDay.of(12, 16)
        val e = MonthDay.of(1, 2)
        assertTrue(DateRules.inRange(MonthDay.of(12, 16), s, e))
        assertTrue(DateRules.inRange(MonthDay.of(12, 31), s, e))
        assertTrue(DateRules.inRange(MonthDay.of(1, 1), s, e))
        assertTrue(DateRules.inRange(MonthDay.of(1, 2), s, e))
        assertFalse(DateRules.inRange(MonthDay.of(1, 3), s, e))
        assertFalse(DateRules.inRange(MonthDay.of(12, 15), s, e))
    }

    @Test
    fun spanCountsBothEnds() {
        assertEquals(18, DateRules.spanDays(MonthDay.of(12, 16), MonthDay.of(1, 2)))
        assertEquals(1, DateRules.spanDays(MonthDay.of(7, 4), MonthDay.of(7, 4)))
    }

    @Test
    fun shortestOverlappingAlbumWins() {
        val all = listOf(newYears, christmas, summer, everyday)
        assertEquals("xm", DateRules.activeAlbum(all, MonthDay.of(12, 25))?.id)
        assertEquals("ny", DateRules.activeAlbum(all, MonthDay.of(12, 30))?.id)
        assertEquals("su", DateRules.activeAlbum(all, MonthDay.of(7, 4))?.id)
    }

    @Test
    fun fallsBackToAlbumWithoutDates() {
        val all = listOf(newYears, everyday)
        assertEquals("ev", DateRules.activeAlbum(all, MonthDay.of(3, 1))?.id)
        assertNull(DateRules.activeAlbum(listOf(newYears), MonthDay.of(3, 1)))
    }

    @Test
    fun emptyAlbumsAreSkipped() {
        val emptyNy = newYears.copy(photos = emptyList())
        assertEquals("ev", DateRules.activeAlbum(listOf(emptyNy, everyday), MonthDay.of(12, 31))?.id)
    }
}
