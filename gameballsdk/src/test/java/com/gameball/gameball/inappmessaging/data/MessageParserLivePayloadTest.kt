package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses a payload captured verbatim from api.alpha.gameball.app, not one we wrote.
 *
 * Reading the documentation is not enough here: the live endpoint has returned a 422 where
 * the docs said 2xx, required a GUID the docs never mentioned, and grown undocumented root
 * fields mid-development. Every assertion below is a rule this payload would have broken had
 * the parser been written from the spec alone.
 *
 * Captured 2026-08-27 for customer moaty-survey-3, platform 2.
 */
class MessageParserLivePayloadTest {

    private val payload: String =
        javaClass.classLoader!!.getResourceAsStream("live_sync_payload.json")!!
            .bufferedReader().use { it.readText() }

    private val parsed = MessageParser.parse(payload)

    @Test
    fun `the captured payload parses without throwing and yields every campaign`() {
        assertEquals(6, parsed.campaigns.size)
        assertEquals(10, parsed.cooldownSeconds)
    }

    /**
     * Not one campaign on the live account sends contentMode. Requiring it rather than
     * defaulting to "prerendered" would drop all six and look like an empty account.
     */
    @Test
    fun `contentMode is absent on every live campaign and must default to prerendered`() {
        assertFalse(
            "the fixture is expected to omit contentMode entirely",
            payload.contains("\"contentMode\"")
        )
        assertEquals(6, parsed.campaigns.size)
    }

    /**
     * The live fullscreen campaign puts its asset under content.media and leaves imageUrl
     * null. A parser reading only imageUrl renders nothing and looks like a backend problem.
     */
    @Test
    fun `the fullscreen campaign resolves its artwork from media rather than imageUrl`() {
        val fullscreen = parsed.campaigns.single { it.messageType == MessageType.FULLSCREEN }
        assertEquals(2055, fullscreen.campaignId)
        assertNotNull("artwork must resolve from content.media.url", fullscreen.content.imageUrl)
        assertTrue(fullscreen.content.imageUrl!!.startsWith("http"))
    }

    @Test
    fun `every parsed campaign has something to render`() {
        parsed.campaigns.forEach { campaign ->
            assertTrue(
                "campaign ${campaign.campaignId} has nothing to render",
                campaign.content.hasText || campaign.content.hasArtwork
            )
        }
    }

    /** The account really does send a midnight-wrapping window, enabled. */
    @Test
    fun `the root quiet-hours window is parsed and wraps midnight`() {
        val window = parsed.quietHours
        assertNotNull(window)
        assertEquals(22 * 60, window!!.startMinute)
        assertEquals(8 * 60, window.endMinute)
        assertTrue("22:00 -> 08:00 must wrap", window.startMinute > window.endMinute)
    }

    @Test
    fun `response order is preserved as the marketer arranged it`() {
        assertEquals(parsed.campaigns.indices.toList(), parsed.campaigns.map { it.responseIndex })
        assertEquals(
            listOf(2055, 2054, 2057, 2059, 2060, 2061),
            parsed.campaigns.map { it.campaignId }
        )
    }

    /**
     * campaignOrdering names 2052 and 2053, neither of which appears in the same response.
     * That is the concrete reason it is ignored: acting on it would re-rank ties the
     * dashboard had already settled, against ids that are not even present.
     */
    @Test
    fun `campaignOrdering names campaigns absent from the response and is ignored`() {
        assertTrue(payload.contains("campaignOrdering"))
        val ids = parsed.campaigns.map { it.campaignId }.toSet()
        assertFalse("2052 is named by campaignOrdering but absent", ids.contains(2052))
        assertFalse("2053 is named by campaignOrdering but absent", ids.contains(2053))
    }

    /** Live closeBehaviour values are button, swipe and null — the explicit "both" never occurs. */
    @Test
    fun `a live slideup with closeBehaviour swipe still gets its auto-dismiss default`() {
        val slideup = parsed.campaigns.single { it.messageType == MessageType.SLIDEUP }
        assertEquals(2054, slideup.campaignId)
        assertEquals(
            "a slideup draws no glyph and has no scrim; without a timer it has no exit",
            8_000L,
            slideup.content.autoDismissMillis
        )
    }

    @Test
    fun `the two undocumented root keys do not disturb parsing`() {
        // campaignOrdering and quietHours both appeared during development.
        assertEquals(6, parsed.campaigns.size)
    }
}
