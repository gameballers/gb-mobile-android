package com.gameball.gameball.engagement

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.TimeZone
import java.util.UUID

class PushEngagementTest {

    // 2026-11-09T18:00:00.123Z
    private val at = 1_794_247_200_123L

    @Test
    fun theNumbersAreTheBackendsOwn() {
        assertEquals(1, PushEngagementType.CLICKED.value)
        assertEquals(2, PushEngagementType.DISMISSED.value)
        assertEquals(3, PushEngagementType.INFLUENCED_OPEN.value)
    }

    @Test
    fun theBodyCarriesTheTokenTypeIdAndTime() {
        val body = pushEngagementBody("tok-1", PushEngagementType.INFLUENCED_OPEN, at, eventUid = "uid-1")
        assertEquals(
            mapOf(
                "clickToken" to "tok-1",
                "engagementType" to 3,
                "eventUid" to "uid-1",
                "occurredAt" to "2026-11-09T18:00:00.123Z"
            ),
            body
        )
    }

    @Test
    fun theTypeGoesOutAsANumber() {
        val json = Gson().toJson(pushEngagementBody("tok-1", PushEngagementType.DISMISSED, at, eventUid = "uid-1"))
        assertEquals(
            """{"clickToken":"tok-1","engagementType":2,"eventUid":"uid-1","occurredAt":"2026-11-09T18:00:00.123Z"}""",
            json
        )
    }

    @Test
    fun eachEventGetsItsOwnUuid() {
        val first = pushEngagementBody("tok-1", PushEngagementType.CLICKED, at)["eventUid"] as String
        val second = pushEngagementBody("tok-1", PushEngagementType.CLICKED, at)["eventUid"] as String
        assertNotEquals(first, second)
        assertEquals(first, UUID.fromString(first).toString())
    }

    @Test
    fun occurredAtIsUtcWhateverTheDeviceTimeZone() {
        val deviceZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            val body = pushEngagementBody("tok-1", PushEngagementType.CLICKED, at)
            assertEquals("2026-11-09T18:00:00.123Z", body["occurredAt"])
        } finally {
            TimeZone.setDefault(deviceZone)
        }
    }
}
