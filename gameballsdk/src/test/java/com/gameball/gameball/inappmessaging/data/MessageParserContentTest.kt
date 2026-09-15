package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.domain.TextAlign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserContentTest {

    private fun parseOne(
        messageType: Int = 2,
        content: String = "{}",
        locale: String = """{ "header": "Hello" }"""
    ): Campaign? = MessageParser.parse(
        """
        { "messages": [ {
            "campaignId": 1, "messageType": $messageType, "contentMode": "prerendered",
            "trigger": { "type": "session_start" },
            "content": $content, "locale": $locale
        } ] }
        """.trimIndent()
    ).campaigns.firstOrNull()

    // --- buttons ---

    @Test
    fun `buttons are paired across content and locale by id`() {
        val campaign = parseOne(
            content = """
                { "buttons": [
                    { "id": "ok", "action": { "type": "dismiss" } },
                    { "id": "cancel", "action": { "type": "dismiss" } } ] }
            """.trimIndent(),
            locale = """
                { "header": "Hi", "buttons": [
                    { "id": "ok", "text": "Track my order" },
                    { "id": "cancel", "text": "Not now" } ] }
            """.trimIndent()
        )!!
        assertEquals(listOf("ok", "cancel"), campaign.content.buttons.map { it.id })
        assertEquals("Track my order", campaign.content.buttons.first().text)
    }

    @Test
    fun `a button styled but not translated is dropped`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok", "action": { "type": "dismiss" } },
                                        { "id": "ghost", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" } ] }"""
        )!!
        assertEquals(listOf("ok"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a button translated but not styled is dropped`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" },
                                                       { "id": "orphan", "text": "Nowhere" } ] }"""
        )!!
        assertEquals(listOf("ok"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a modal keeps the first two buttons and does not drop the campaign`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } },
                                        { "id": "b", "action": { "type": "dismiss" } },
                                        { "id": "c", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "a", "text": "A" },
                                                       { "id": "b", "text": "B" },
                                                       { "id": "c", "text": "C" } ] }"""
        )!!
        assertEquals(listOf("a", "b"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a fullscreen has no button cap`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } },
                                        { "id": "b", "action": { "type": "dismiss" } },
                                        { "id": "c", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "a", "text": "A" },
                                                       { "id": "b", "text": "B" },
                                                       { "id": "c", "text": "C" } ] }"""
        )!!
        assertEquals(3, campaign.content.buttons.size)
    }

    @Test
    fun `a slideup drops every button`() {
        val campaign = parseOne(
            messageType = 1,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "message": "Nice pick", "buttons": [ { "id": "a", "text": "A" } ] }"""
        )!!
        assertTrue(campaign.content.buttons.isEmpty())
    }

    @Test
    fun `a button with no usable action falls back to dismiss`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok" } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" } ] }"""
        )!!
        assertEquals(MessageAction.Dismiss, campaign.content.buttons.first().action)
    }

    // --- actions ---

    @Test
    fun `a null message action leaves the surface inert`() {
        assertNull(parseOne(content = """{ "action": null }""")!!.content.clickAction)
        assertNull(parseOne(content = "{}")!!.content.clickAction)
    }

    @Test
    fun `navigate carries a bare route with no leading slash`() {
        val action = parseOne(
            content = """{ "action": { "type": "navigate", "route": "orders" } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.Navigate("orders", null), action)
    }

    @Test
    fun `open_url carries the url and the external flag`() {
        val action = parseOne(
            content = """{ "action": { "type": "open_url", "url": "https://x/y", "external": true } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.OpenUrl("https://x/y", true), action)
    }

    @Test
    fun `external defaults to false`() {
        val action = parseOne(
            content = """{ "action": { "type": "open_url", "url": "https://x/y" } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.OpenUrl("https://x/y", false), action)
    }

    @Test
    fun `the unimplemented action types parse as unsupported`() {
        listOf("log_event", "log_attribute", "request_push_permission").forEach { type ->
            val action = parseOne(content = """{ "action": { "type": "$type" } }""")!!
                .content.clickAction
            assertEquals(MessageAction.Unsupported(type), action)
        }
    }

    // --- artwork resolution ---

    @Test
    fun `fullscreen prefers media url over imageUrl`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "imageUrl": "https://x/a.jpg",
                           "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `fullscreen falls back to imageUrl when media is absent`() {
        val campaign = parseOne(messageType = 3, content = """{ "imageUrl": "https://x/a.jpg" }""")!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a modal prefers imageUrl over media url`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "imageUrl": "https://x/a.jpg",
                           "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    /** The live QA campaign puts its image under media and leaves imageUrl null. */
    @Test
    fun `a modal falls back to media url when imageUrl is absent`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a media type of video is ignored`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "media": { "type": "video", "url": "https://x/v.mp4" },
                           "imageUrl": "https://x/a.jpg" }"""
        )!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a media entry with no type is treated as an image`() {
        val campaign = parseOne(messageType = 3, content = """{ "media": { "url": "https://x/b.jpg" } }""")!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a blank url is treated as absent`() {
        val campaign = parseOne(
            content = """{ "imageUrl": "   ", "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    // --- layout ---

    @Test
    fun `every layout spelling maps and an unknown one falls back to the default`() {
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "layout": "text_with_image", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(messageType = 3, content = """{ "layout": "image_and_text", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.IMAGE_ONLY,
            parseOne(content = """{ "layout": "image_only", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "layout": "carousel", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
    }

    /**
     * The one place a declared layout may be overridden. image_only never references header
     * or body, so with no artwork it renders bare background plus buttons, counts an
     * impression and reports nothing wrong.
     */
    @Test
    fun `image_only with no artwork falls back to the stacked composition`() {
        val campaign = parseOne(content = """{ "layout": "image_only" }""")!!
        assertEquals(MessageLayout.DEFAULT, campaign.content.layout)
        assertEquals("Hello", campaign.content.header)
    }

    @Test
    fun `absent copy does not imply image_only`() {
        val campaign = parseOne(
            content = """{ "imageUrl": "https://x/a.jpg" }""",
            locale = "{}"
        )!!
        assertEquals(MessageLayout.DEFAULT, campaign.content.layout)
    }

    // --- copy sources ---

    @Test
    fun `body comes from locale message, else locale body`() {
        assertEquals("From message",
            parseOne(locale = """{ "header": "H", "message": "From message" }""")!!.content.body)
        assertEquals("From body",
            parseOne(locale = """{ "header": "H", "body": "From body" }""")!!.content.body)
    }

    @Test
    fun `slideup copy falls back to the header so a mis-filled campaign still says something`() {
        val campaign = parseOne(messageType = 1, locale = """{ "header": "Only a header" }""")!!
        assertEquals("Only a header", campaign.content.body)
    }

    // --- dismissal ---

    @Test
    fun `closeBehaviour both and null give a glyph and a dismissing scrim`() {
        listOf(""""both"""", "null").forEach { value ->
            val content = parseOne(content = """{ "closeBehaviour": $value }""")!!.content
            assertTrue(content.showCloseButton)
            assertTrue(content.dismissOnScrimTap)
        }
    }

    @Test
    fun `closeBehaviour button gives a glyph and a non-dismissing scrim`() {
        val content = parseOne(content = """{ "closeBehaviour": "button" }""")!!.content
        assertTrue(content.showCloseButton)
        assertFalse(content.dismissOnScrimTap)
    }

    @Test
    fun `closeBehaviour swipe on a modal gives a dismissing scrim and no glyph`() {
        val content = parseOne(messageType = 2, content = """{ "closeBehaviour": "swipe" }""")!!.content
        assertFalse(content.showCloseButton)
        assertTrue(content.dismissOnScrimTap)
    }

    /**
     * A fullscreen has no scrim and no swipe gesture, so obeying "swipe" literally leaves only
     * the system back gesture — which does not exist on iOS. The parser promotes and logs
     * rather than shipping a trap.
     */
    @Test
    fun `closeBehaviour swipe is promoted to both on a fullscreen`() {
        val content = parseOne(messageType = 3, content = """{ "closeBehaviour": "swipe" }""")!!.content
        assertTrue("a fullscreen must keep its glyph", content.showCloseButton)
    }

    @Test
    fun `an unrecognised closeBehaviour falls back to both`() {
        val content = parseOne(content = """{ "closeBehaviour": "telepathy" }""")!!.content
        assertTrue(content.showCloseButton)
        assertTrue(content.dismissOnScrimTap)
    }

    // --- auto dismiss ---

    @Test
    fun `a slideup with no duration gets the 8 second default`() {
        assertEquals(8_000L, parseOne(messageType = 1, locale = """{ "message": "Hi" }""")!!
            .content.autoDismissMillis)
    }

    @Test
    fun `an explicit zero means stay until dismissed and is not overridden`() {
        assertNull(parseOne(
            messageType = 1,
            content = """{ "autoDismissSeconds": 0 }""",
            locale = """{ "message": "Hi" }"""
        )!!.content.autoDismissMillis)
    }

    @Test
    fun `modal and fullscreen get no timer when none is set`() {
        assertNull(parseOne(messageType = 2)!!.content.autoDismissMillis)
        assertNull(parseOne(messageType = 3)!!.content.autoDismissMillis)
    }

    @Test
    fun `a fractional duration is rounded to milliseconds`() {
        assertEquals(2_500L,
            parseOne(content = """{ "autoDismissSeconds": 2.5 }""")!!.content.autoDismissMillis)
    }

    // --- colours, alignment, slide position, extras ---

    @Test
    fun `colours are parsed into their slots and absent ones stay null`() {
        val colors = parseOne(
            content = """{ "colors": { "background": "#FFFFFF", "text": "#1F2937",
                                       "header": "#111827", "closeButton": null,
                                       "border": null, "frame": null } }"""
        )!!.content.colors
        assertEquals(0xFFFFFFFF.toInt(), colors.background)
        assertEquals(0xFF1F2937.toInt(), colors.text)
        assertEquals(0xFF111827.toInt(), colors.header)
        assertNull(colors.closeButton)
        assertNull(colors.border)
        assertNull(colors.frame)
    }

    @Test
    fun `a malformed colour costs only its own slot`() {
        val colors = parseOne(
            content = """{ "colors": { "background": "chartreuse", "text": "#1F2937" } }"""
        )!!.content.colors
        assertNull(colors.background)
        assertEquals(0xFF1F2937.toInt(), colors.text)
    }

    @Test
    fun `text alignment is read per slot`() {
        val content = parseOne(
            content = """{ "textAlignment": { "header": "center", "body": "start" } }"""
        )!!.content
        assertEquals(TextAlign.CENTER, content.headerAlign)
        assertEquals(TextAlign.START, content.bodyAlign)
    }

    @Test
    fun `slideFrom defaults to bottom`() {
        assertEquals(SlidePosition.BOTTOM, parseOne(messageType = 1, locale = """{ "message": "Hi" }""")!!
            .content.slidePosition)
        assertEquals(SlidePosition.TOP, parseOne(
            messageType = 1,
            content = """{ "slideFrom": "top" }""",
            locale = """{ "message": "Hi" }"""
        )!!.content.slidePosition)
    }

    @Test
    fun `extras coerce non-string values and drop nulls`() {
        val extras = parseOne(
            content = """{ "extras": { "a": "text", "b": 42, "c": true, "d": 1.5, "e": null } }"""
        )!!.content.extras
        assertEquals("text", extras["a"])
        assertEquals("42", extras["b"])
        assertEquals("true", extras["c"])
        assertEquals("1.5", extras["d"])
        assertFalse(extras.containsKey("e"))
    }
}
