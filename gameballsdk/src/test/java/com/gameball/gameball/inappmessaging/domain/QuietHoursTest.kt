package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class QuietHoursTest {

    /** A device three hours ahead of UTC. A fixed offset, so no daylight saving moves it. */
    private val device: TimeZone = TimeZone.getTimeZone("GMT+03:00")

    /**
     * Builds the instant at which [zone]'s wall clock reads [hour]:[minute]. The window is read
     * on the device's clock, so test instants are built on that same clock — otherwise the same
     * test would pass in Cairo and fail in Los Angeles.
     */
    private fun at(hour: Int, minute: Int, zone: TimeZone = device): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.AUGUST, 27, hour, minute, 0)
        }.timeInMillis

    private fun QuietHours.containsAt(hour: Int, minute: Int) = contains(at(hour, minute), device)

    @Test
    fun `a same-day window contains only its own hours`() {
        val window = QuietHours.from(true, "09:00", "17:00")!!
        assertFalse(window.containsAt(8, 59))
        assertTrue(window.containsAt(9, 0))
        assertTrue(window.containsAt(12, 0))
        assertTrue(window.containsAt(16, 59))
        assertFalse(window.containsAt(17, 0))
    }

    @Test
    fun `the window is half-open so adjacent windows do not overlap`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue("the start minute is inside", window.containsAt(22, 0))
        assertFalse("the end minute is not", window.containsAt(8, 0))
    }

    /** 22:00 -> 08:00 is what the backend actually sends. It is two ranges, not one. */
    @Test
    fun `a window wrapping midnight covers both sides`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue(window.containsAt(22, 30))
        assertTrue(window.containsAt(23, 59))
        assertTrue(window.containsAt(0, 0))
        assertTrue(window.containsAt(3, 0))
        assertTrue(window.containsAt(7, 59))
        assertFalse(window.containsAt(8, 1))
        assertFalse(window.containsAt(12, 0))
        assertFalse(window.containsAt(21, 59))
    }

    @Test
    fun `seconds in the wire value are tolerated`() {
        val window = QuietHours.from(true, "22:00:00", "08:00:00")!!
        assertTrue(window.containsAt(23, 0))
        assertFalse(window.containsAt(9, 0))
    }

    /**
     * The contract: "daily do-not-disturb in the device's local clock (never UTC)". At UTC+3 the
     * two readings disagree for six hours of every day, in both directions.
     */
    @Test
    fun `the window is read on the device's local clock, not UTC`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        // 23:00 on the device is 20:00 UTC: inside locally, outside in UTC.
        assertTrue(window.contains(at(23, 0), device))
        // 09:00 on the device is 06:00 UTC: outside locally, inside in UTC.
        assertFalse(window.contains(at(9, 0), device))
    }

    @Test
    fun `with no zone given, the device's own zone is used`() {
        val originalZone = TimeZone.getDefault()
        try {
            val newYorkWinter = TimeZone.getTimeZone("GMT-05:00")
            TimeZone.setDefault(newYorkWinter)
            val window = QuietHours.from(true, "22:00", "08:00")!!
            // 21:00 on the device is 02:00 UTC, and 07:00 on the device is 12:00 UTC.
            assertFalse(window.contains(at(21, 0, newYorkWinter)))
            assertTrue(window.contains(at(7, 0, newYorkWinter)))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    /**
     * Zero-length and twenty-four-hours look identical on the wire. Silencing an entire
     * account over a typo is the worse reading, so the window is refused.
     */
    @Test
    fun `start equal to end is refused`() {
        assertNull(QuietHours.from(true, "08:00", "08:00"))
    }

    @Test
    fun `disabled, absent and malformed all mean no window`() {
        assertNull(QuietHours.from(false, "22:00", "08:00"))
        assertNull(QuietHours.from(null, "22:00", "08:00"))
        assertNull(QuietHours.from(true, null, "08:00"))
        assertNull(QuietHours.from(true, "22:00", null))
        assertNull(QuietHours.from(true, "", ""))
        assertNull(QuietHours.from(true, "25:00", "08:00"))
        assertNull(QuietHours.from(true, "22:61", "08:00"))
        assertNull(QuietHours.from(true, "ten o'clock", "08:00"))
        assertNull(QuietHours.from(true, "2200", "0800"))
    }

    @Test
    fun `minute of day is read on the clock of the zone it is given`() {
        val losAngelesSummer = TimeZone.getTimeZone("GMT-07:00")
        assertEquals(22 * 60 + 30, QuietHours.minuteOfDay(at(22, 30, losAngelesSummer), losAngelesSummer))
        assertEquals(22 * 60 + 30, QuietHours.minuteOfDay(at(22, 30, device), device))
        assertEquals(19 * 60 + 30, QuietHours.minuteOfDay(at(22, 30, device), TimeZone.getTimeZone("UTC")))
    }
}
