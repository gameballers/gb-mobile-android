package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class IamTimeTest {

    /** 2026-08-27T10:30:00.123Z */
    private val instant = 1787826600123L

    @Test
    fun `formats as ISO-8601 UTC with millisecond precision`() {
        assertEquals("2026-08-27T10:30:00.123Z", IamTime.toIso8601Utc(instant))
    }

    /**
     * L3: on an Arabic-locale device a default-locale SimpleDateFormat emits Arabic-Indic
     * digits, which is not valid ISO-8601 and 400s the entire analytics batch.
     */
    @Test
    fun `formats in ASCII digits with the default locale set to Arabic`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale("ar", "EG"))
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            val formatted = IamTime.toIso8601Utc(instant)
            assertEquals("2026-08-27T10:30:00.123Z", formatted)
            assertTrue("expected ASCII digits, got $formatted", formatted.all { it.code < 128 })
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun `parses the format it emits`() {
        assertEquals(instant, IamTime.parseIso8601("2026-08-27T10:30:00.123Z"))
    }

    @Test
    fun `parses without milliseconds`() {
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T10:30:00Z"))
    }

    @Test
    fun `parses a numeric offset, normalising the colon form`() {
        // 13:30 at +03:00 is the same instant as 10:30Z
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T13:30:00+03:00"))
    }

    @Test
    fun `treats a missing zone as UTC`() {
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T10:30:00"))
    }

    @Test
    fun `returns null rather than throwing on junk`() {
        assertNull(IamTime.parseIso8601(null))
        assertNull(IamTime.parseIso8601(""))
        assertNull(IamTime.parseIso8601("   "))
        assertNull(IamTime.parseIso8601("not a date"))
        assertNull(IamTime.parseIso8601("2026-13-45T99:99:99Z"))
    }
}
