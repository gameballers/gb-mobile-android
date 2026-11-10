package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class QuietHoursTest {

    /**
     * Builds an instant from a UTC wall clock. Test instants must be built in UTC — a local
     * literal is judged by its UTC equivalent, so the same test would pass in Cairo and fail
     * in Los Angeles.
     */
    private fun utc(hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.AUGUST, 27, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `a same-day window contains only its own hours`() {
        val window = QuietHours.from(true, "09:00", "17:00")!!
        assertFalse(window.contains(utc(8, 59)))
        assertTrue(window.contains(utc(9, 0)))
        assertTrue(window.contains(utc(12, 0)))
        assertTrue(window.contains(utc(16, 59)))
        assertFalse(window.contains(utc(17, 0)))
    }

    @Test
    fun `the window is half-open so adjacent windows do not overlap`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue("the start minute is inside", window.contains(utc(22, 0)))
        assertFalse("the end minute is not", window.contains(utc(8, 0)))
    }

    /** 22:00 -> 08:00 is what the backend actually sends. It is two ranges, not one. */
    @Test
    fun `a window wrapping midnight covers both sides`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue(window.contains(utc(22, 30)))
        assertTrue(window.contains(utc(23, 59)))
        assertTrue(window.contains(utc(0, 0)))
        assertTrue(window.contains(utc(3, 0)))
        assertTrue(window.contains(utc(7, 59)))
        assertFalse(window.contains(utc(8, 1)))
        assertFalse(window.contains(utc(12, 0)))
        assertFalse(window.contains(utc(21, 59)))
    }

    @Test
    fun `seconds in the wire value are tolerated`() {
        val window = QuietHours.from(true, "22:00:00", "08:00:00")!!
        assertTrue(window.contains(utc(23, 0)))
        assertFalse(window.contains(utc(9, 0)))
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
    fun `minute of day is computed in UTC regardless of the device timezone`() {
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(22 * 60 + 30, QuietHours.minuteOfDayUtc(utc(22, 30)))
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            assertEquals(22 * 60 + 30, QuietHours.minuteOfDayUtc(utc(22, 30)))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }
}
