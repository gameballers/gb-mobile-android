package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.DEFAULT_COOLDOWN_SECONDS
import com.gameball.gameball.inappmessaging.domain.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserEnvelopeTest {

    /** A campaign with only the fields the parser requires. */
    private fun minimalCampaign(
        campaignId: String = "2055",
        messageType: String = "2",
        extra: String = ""
    ) = """
        {
          "campaignId": $campaignId,
          "messageType": $messageType,
          "contentMode": "prerendered",
          "trigger": { "type": "session_start" },
          "content": {},
          "locale": { "header": "Hello" }
          $extra
        }
    """.trimIndent()

    private fun payload(vararg campaigns: String) = """
        { "cooldownSeconds": 10, "messages": [ ${campaigns.joinToString(",")} ] }
    """.trimIndent()

    @Test
    fun `a minimal campaign parses`() {
        val result = MessageParser.parse(payload(minimalCampaign()))
        assertEquals(1, result.campaigns.size)
        val campaign = result.campaigns.first()
        assertEquals(2055, campaign.campaignId)
        assertEquals(MessageType.MODAL, campaign.messageType)
        assertEquals(0, campaign.priority)
        assertEquals(false, campaign.isTest)
        assertNull(campaign.expiresAtMillis)
        assertEquals(0, campaign.responseIndex)
    }

    @Test
    fun `cooldownSeconds is read from the root and defaults to 30`() {
        assertEquals(10, MessageParser.parse(payload(minimalCampaign())).cooldownSeconds)
        assertEquals(
            DEFAULT_COOLDOWN_SECONDS,
            MessageParser.parse("""{ "messages": [] }""").cooldownSeconds
        )
    }

    @Test
    fun `quiet hours are read from the root`() {
        val json = """
            {
              "cooldownSeconds": 30,
              "quietHours": { "enabled": true, "start": "22:00", "end": "08:00" },
              "messages": []
            }
        """.trimIndent()
        val window = MessageParser.parse(json).quietHours
        assertNotNull(window)
        assertEquals(22 * 60, window!!.startMinute)
        assertEquals(8 * 60, window.endMinute)
    }

    @Test
    fun `unknown root keys are ignored`() {
        val json = """
            {
              "cooldownSeconds": 30,
              "campaignOrdering": [2052, 2053],
              "somethingTheBackendAddedLastWeek": { "a": 1 },
              "messages": [ ${minimalCampaign()} ]
            }
        """.trimIndent()
        assertEquals(1, MessageParser.parse(json).campaigns.size)
    }

    @Test
    fun `a missing campaignId drops the campaign`() {
        val json = """
            { "messages": [ { "messageType": 2, "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `a missing messageType drops the campaign`() {
        val json = """
            { "messages": [ { "campaignId": 1, "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `an unknown messageType is kept and marked unsupported`() {
        val result = MessageParser.parse(payload(minimalCampaign(messageType = "99")))
        assertEquals(1, result.campaigns.size)
        assertEquals(MessageType.UNSUPPORTED, result.campaigns.first().messageType)
        assertEquals(99, result.campaigns.first().rawMessageType)
    }

    @Test
    fun `a non-prerendered contentMode drops the campaign`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2, "contentMode": "remote",
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `an absent contentMode defaults to prerendered and is kept`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertEquals(1, MessageParser.parse(json).campaigns.size)
    }

    @Test
    fun `a campaign with no header, body or image is dropped`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {}, "locale": {} } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `a slideup with an icon but no text is dropped`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 1,
              "trigger": { "type": "session_start" },
              "content": { "iconUrl": "https://x/i.png" }, "locale": {} } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `expiresAt is parsed and a null one stays null`() {
        val withExpiry = minimalCampaign(extra = """, "expiresAt": "2026-12-31T23:59:59Z"""")
        assertEquals(
            IamTime.parseIso8601("2026-12-31T23:59:59Z"),
            MessageParser.parse(payload(withExpiry)).campaigns.first().expiresAtMillis
        )
        val nullExpiry = minimalCampaign(extra = """, "expiresAt": null""")
        assertNull(MessageParser.parse(payload(nullExpiry)).campaigns.first().expiresAtMillis)
    }

    @Test
    fun `response index records the marketer's dashboard order`() {
        val result = MessageParser.parse(
            payload(minimalCampaign("10"), minimalCampaign("20"), minimalCampaign("30"))
        )
        assertEquals(listOf(10, 20, 30), result.campaigns.map { it.campaignId })
        assertEquals(listOf(0, 1, 2), result.campaigns.map { it.responseIndex })
    }

    @Test
    fun `one bad campaign does not take the others with it`() {
        val bad = """{ "messageType": 2, "trigger": { "type": "session_start" } }"""
        val result = MessageParser.parse(payload(bad, minimalCampaign("77")))
        assertEquals(1, result.campaigns.size)
        assertEquals(77, result.campaigns.first().campaignId)
    }

    // --- the parser must never throw ---

    @Test
    fun `malformed json returns an empty result`() {
        assertEquals(0, MessageParser.parse("{ not json").campaigns.size)
        assertEquals(DEFAULT_COOLDOWN_SECONDS, MessageParser.parse("{ not json").cooldownSeconds)
    }

    @Test
    fun `a non-object root returns an empty result`() {
        assertTrue(MessageParser.parse("[]").campaigns.isEmpty())
        assertTrue(MessageParser.parse("\"a string\"").campaigns.isEmpty())
        assertTrue(MessageParser.parse("null").campaigns.isEmpty())
    }

    @Test
    fun `a missing or wrongly typed messages array returns an empty result`() {
        assertTrue(MessageParser.parse("{}").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": null }""").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": {} }""").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": "nope" }""").campaigns.isEmpty())
    }

    @Test
    fun `null and empty input return an empty result`() {
        assertTrue(MessageParser.parse(null).campaigns.isEmpty())
        assertTrue(MessageParser.parse("").campaigns.isEmpty())
    }

    @Test
    fun `wrongly typed scalars do not throw`() {
        val json = """
            { "cooldownSeconds": "ten",
              "quietHours": "yes",
              "messages": [ { "campaignId": "not a number", "messageType": 2,
                "trigger": { "type": "session_start" }, "content": {},
                "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        val result = MessageParser.parse(json)
        assertEquals(DEFAULT_COOLDOWN_SECONDS, result.cooldownSeconds)
        assertNull(result.quietHours)
        assertTrue(result.campaigns.isEmpty())
    }
}
