package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.CampaignKey
import com.gameball.gameball.inappmessaging.domain.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCampaignParserTest {

    /** A game entry shaped exactly as the contract's worked example, meaningless fields included. */
    private fun wheel(extra: String = "", overrides: Map<String, String> = emptyMap()): String {
        val fields = linkedMapOf(
            "campaignType" to "2",
            "campaignId" to "0",
            "variationId" to "0",
            "name" to "\"Spin the Wheel\"",
            "priority" to "0",
            "messageType" to "2",
            "trigger" to """{ "type": "session_start", "repeatable": true }""",
            "content" to "null",
            "locale" to "null",
            "isTest" to "false",
            "gameCampaignId" to "9645",
            "gameCampaignType" to "\"wheel\""
        ) + overrides
        val body = fields.entries.joinToString(",") { "\"${it.key}\": ${it.value}" }
        return "{ $body $extra }"
    }

    private fun payload(vararg campaigns: String) =
        """{ "cooldownSeconds": 10, "messages": [ ${campaigns.joinToString(",")} ] }"""

    private fun parse(entry: String) = MessageParser.parse(payload(entry)).campaigns

    @Test
    fun `a game entry parses with no content of its own`() {
        val campaigns = parse(wheel())
        assertEquals(1, campaigns.size)

        val widget = campaigns.first().widget!!
        assertEquals(9645, widget.gameCampaignId)
        assertEquals("wheel", widget.gameCampaignType)
        assertEquals("details_wheel_9645", widget.openDetail)
    }

    @Test
    fun `a game entry is identified by its game id, never by campaignId 0`() {
        assertEquals(CampaignKey.game(9645), parse(wheel()).first().key)
    }

    @Test
    fun `a game entry is labelled by its game, not by campaign zero`() {
        assertEquals("game:9645", parse(wheel()).first().label)
    }

    @Test
    fun `an unknown campaignType is dropped, not coerced`() {
        assertTrue(parse(wheel(overrides = mapOf("campaignType" to "7"))).isEmpty())
    }

    @Test
    fun `an unrecognised gameCampaignType is dropped`() {
        assertTrue(parse(wheel(overrides = mapOf("gameCampaignType" to "\"roulette\""))).isEmpty())
    }

    @Test
    fun `a game entry with no gameCampaignId is dropped`() {
        assertTrue(parse(wheel(overrides = mapOf("gameCampaignId" to "0"))).isEmpty())
    }

    @Test
    fun `a message with nothing to render is still dropped`() {
        // The exemption is keyed off campaignType, not off the fields being absent, so a
        // malformed message keeps failing for the right reason.
        val entry = wheel(overrides = mapOf("campaignType" to "1", "campaignId" to "2215"))
        assertTrue(parse(entry).isEmpty())
    }

    @Test
    fun `an entry with no campaignType is read as a message`() {
        // Every payload written before the field existed looks like this. Defaulting to the kind
        // that has always existed is safe; defaulting to a game would open a campaign that is
        // not there.
        val legacy = """
            {
              "campaignId": 2215, "messageType": 2, "contentMode": "prerendered",
              "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hello" }
            }
        """.trimIndent()
        val campaigns = parse(legacy)
        assertEquals(1, campaigns.size)
        assertFalse(campaigns.first().isWidget)
        assertEquals(CampaignKey.message(2215), campaigns.first().key)
    }

    @Test
    fun `a game whose conditions are joined by OR is kept`() {
        val entry = wheel(overrides = mapOf(
            "trigger" to """{ "type": "event", "name": "purchase", "repeatable": true,
                "metadataLogicalOperator": "OR",
                "metadataFilters": [
                  { "metadataId": 1, "name": "price", "value": "100", "operator": "Greater" },
                  { "metadataId": 2, "name": "currency", "value": "USD", "operator": "Is" } ] }"""
        ))
        assertEquals(1, parse(entry).size)
    }

    @Test
    fun `a screen-triggered game is dropped until screens are supported`() {
        val entry = wheel(overrides = mapOf(
            "trigger" to """{ "type": "screen", "name": "cart", "repeatable": true }"""
        ))
        assertTrue(parse(entry).isEmpty())
    }

    @Test
    fun `a fullscreen game is kept and marked fullscreen`() {
        // The earlier reading that fullscreen clips was an artefact of measuring a 1.12-aspect
        // box. A portrait screen is about 0.46, comfortably inside the ratio at which the widget
        // starts clipping.
        val campaigns = parse(wheel(overrides = mapOf("messageType" to "3")))
        assertEquals(1, campaigns.size)
        assertEquals(MessageType.FULLSCREEN, campaigns.first().messageType)
        assertTrue(campaigns.first().isWidget)
    }

    @Test
    fun `a delay or dismiss limit on the entry changes nothing`() {
        // A game opens when its trigger fires and is offered by the same frequency rules as a
        // message: there is no delay and no dismissal limit.
        val plain = parse(wheel()).first().widget
        val withExtras = parse(
            wheel(overrides = mapOf("delay" to "3", "dismissLimit" to "2"))
        ).first().widget
        assertEquals(plain, withExtras)
    }

    @Test
    fun `a game and a message parse from one response`() {
        val message = """
            {
              "campaignType": 1, "campaignId": 2215, "messageType": 2,
              "contentMode": "prerendered", "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hello" }
            }
        """.trimIndent()
        val campaigns = MessageParser.parse(payload(wheel(), message)).campaigns

        assertEquals(2, campaigns.size)
        assertTrue(campaigns[0].isWidget)
        assertFalse(campaigns[1].isWidget)
    }
}
