package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MessageSelectorTest {

    private val now = 1_800_000_000_000L   // a fixed instant; nothing here waits

    private fun content() = MessageContent(
        header = "H", body = "B", imageUrl = null, iconUrl = null,
        layout = MessageLayout.DEFAULT, colors = MessageColors.EMPTY, buttons = emptyList(),
        clickAction = null, showCloseButton = true, dismissOnScrimTap = true,
        slidePosition = SlidePosition.BOTTOM, orientation = MessageOrientation.ANY,
        autoDismissMillis = null, headerAlign = null, bodyAlign = null, extras = emptyMap()
    )

    private fun campaign(
        id: Int,
        priority: Int = 0,
        index: Int = 0,
        type: MessageType = MessageType.MODAL,
        trigger: Trigger = Trigger(TriggerType.SESSION_START),
        expiresAtMillis: Long? = null
    ) = Campaign(
        campaignId = id, variationId = null, dispatchId = null, name = "c$id",
        priority = priority, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = expiresAtMillis, isTest = false, trigger = trigger,
        content = content(), responseIndex = index
    )

    private fun select(
        campaigns: List<Campaign>,
        occurrence: TriggerOccurrence = TriggerOccurrence.SessionStart,
        history: DisplayHistorySnapshot = DisplayHistorySnapshot(),
        nowMillis: Long = now,
        cooldownSeconds: Int = 30,
        quietHours: QuietHours? = null,
        artworkReady: (Campaign) -> Boolean = { true }
    ) = MessageSelector.select(
        occurrence, campaigns, history, nowMillis, cooldownSeconds, quietHours, artworkReady
    )

    // --- trigger matching ---

    @Test
    fun `session start selects a session-start campaign`() {
        assertEquals(1, select(listOf(campaign(1)))?.campaignId)
    }

    @Test
    fun `a named event selects a campaign listening for it`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertEquals(1, select(listOf(c), TriggerOccurrence.Event("place_order"))?.campaignId)
    }

    @Test
    fun `a non-matching event selects nothing`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertNull(select(listOf(c), TriggerOccurrence.Event("view_product_page")))
    }

    @Test
    fun `an event does not fire a session-start campaign, and vice versa`() {
        val sessionStart = campaign(1)
        val event = campaign(2, trigger = Trigger(TriggerType.EVENT, eventName = "x"))
        assertNull(select(listOf(sessionStart), TriggerOccurrence.Event("x")))
        assertNull(select(listOf(event), TriggerOccurrence.SessionStart))
    }

    @Test
    fun `event names match exactly, not case-insensitively`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertNull(select(listOf(c), TriggerOccurrence.Event("Place_Order")))
    }

    @Test
    fun `filters gate an event campaign`() {
        val c = campaign(
            1,
            trigger = Trigger(
                TriggerType.EVENT, eventName = "purchase",
                filters = listOf(MetadataFilter("price", FilterOperator.GREATER_THAN, 100))
            )
        )
        assertEquals(1, select(
            listOf(c), TriggerOccurrence.Event("purchase", mapOf("price" to 150))
        )?.campaignId)
        assertNull(select(listOf(c), TriggerOccurrence.Event("purchase", mapOf("price" to 50))))
        assertNull(select(listOf(c), TriggerOccurrence.Event("purchase", emptyMap())))
    }

    // --- expiry ---

    /**
     * Checked at selection, not only at fetch: campaigns are cached for the session, so one
     * fetched at 23:58 would otherwise fire all night and keep firing after it was paused.
     */
    @Test
    fun `an expired campaign is never selected`() {
        assertNull(select(listOf(campaign(1, expiresAtMillis = now - 1))))
        assertNull(select(listOf(campaign(1, expiresAtMillis = now))))
        assertEquals(1, select(listOf(campaign(1, expiresAtMillis = now + 1)))?.campaignId)
    }

    // --- repeat rules ---

    @Test
    fun `a non-repeatable campaign is never selected twice`() {
        val c = campaign(1)
        val history = DisplayHistorySnapshot(
            perCampaign = mapOf(1 to DisplayRecord(now - 10_000_000L, 1)),
            lastDisplayAtMillis = now - 10_000_000L
        )
        assertNull(select(listOf(c), history = history))
    }

    @Test
    fun `a repeatable campaign respects minIntervalSeconds`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = 300
        ))
        val occurrence = TriggerOccurrence.Event("x")

        val tooSoon = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 299_000L, 1)), now - 299_000L
        )
        assertNull(select(listOf(c), occurrence, tooSoon, cooldownSeconds = 0))

        val longEnough = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 300_000L, 1)), now - 300_000L
        )
        assertEquals(1, select(listOf(c), occurrence, longEnough, cooldownSeconds = 0)?.campaignId)
    }

    @Test
    fun `a repeatable campaign with no interval fires on every occurrence`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = null
        ))
        val history = DisplayHistorySnapshot(mapOf(1 to DisplayRecord(now - 1L, 5)), now - 1L)
        assertEquals(1, select(
            listOf(c), TriggerOccurrence.Event("x"), history, cooldownSeconds = 0
        )?.campaignId)
    }

    // --- the global floor ---

    @Test
    fun `inside the cooldown floor nothing is selected`() {
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 29_000L)
        assertNull(select(listOf(campaign(1)), history = history, cooldownSeconds = 30))
    }

    @Test
    fun `outside the cooldown floor the same campaign is selected`() {
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 30_000L)
        assertEquals(1, select(listOf(campaign(1)), history = history, cooldownSeconds = 30)?.campaignId)
    }

    /** The floor is global, not per campaign. */
    @Test
    fun `the floor blocks a campaign that has never displayed`() {
        val history = DisplayHistorySnapshot(
            perCampaign = mapOf(99 to DisplayRecord(now - 5_000L, 1)),
            lastDisplayAtMillis = now - 5_000L
        )
        assertNull(select(listOf(campaign(1)), history = history, cooldownSeconds = 30))
    }

    // --- ranking ---

    @Test
    fun `the highest priority wins`() {
        val campaigns = listOf(
            campaign(1, priority = 3, index = 0),
            campaign(2, priority = 7, index = 1),
            campaign(3, priority = 5, index = 2)
        )
        assertEquals(2, select(campaigns)?.campaignId)
    }

    /**
     * The tie-break is response order and it is meaningful, not merely deterministic: the
     * backend returns campaigns in the sequence the marketer arranged in the dashboard.
     * Here the response order deliberately contradicts ascending campaignId, which is the
     * comparator a well-meaning refactor would reach for.
     */
    @Test
    fun `ties break on response order even when it contradicts campaignId order`() {
        val campaigns = listOf(
            campaign(900, priority = 5, index = 0),
            campaign(100, priority = 5, index = 1),
            campaign(500, priority = 5, index = 2)
        )
        assertEquals(900, select(campaigns)?.campaignId)
    }

    @Test
    fun `response order breaks ties regardless of the order the list is built in`() {
        val campaigns = listOf(
            campaign(100, priority = 5, index = 1),
            campaign(900, priority = 5, index = 0)
        )
        assertEquals(900, select(campaigns)?.campaignId)
    }

    // --- gates that let a lower-priority campaign win ---

    /**
     * Unsupported types are filtered at selection rather than refused at display, so a usable
     * lower-priority campaign wins instead of the occurrence being wasted.
     */
    @Test
    fun `an unsupported type is filtered so a lower-priority supported campaign wins`() {
        val campaigns = listOf(
            campaign(1, priority = 10, index = 0, type = MessageType.UNSUPPORTED),
            campaign(2, priority = 1, index = 1, type = MessageType.MODAL)
        )
        assertEquals(2, select(campaigns)?.campaignId)
    }

    @Test
    fun `a campaign whose artwork is not ready is passed over`() {
        val campaigns = listOf(
            campaign(1, priority = 10, index = 0),
            campaign(2, priority = 1, index = 1)
        )
        assertEquals(2, select(campaigns, artworkReady = { it.campaignId != 1 })?.campaignId)
    }

    @Test
    fun `nothing eligible selects nothing`() {
        assertNull(select(emptyList()))
        assertNull(select(listOf(campaign(1, type = MessageType.UNSUPPORTED))))
    }

    // --- quiet hours ---

    private fun utcInstantAt(hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2026, Calendar.AUGUST, 27, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `inside quiet hours nothing displays and outside the same campaign does`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertNull(select(
            listOf(campaign(1)), nowMillis = utcInstantAt(23, 0), quietHours = window
        ))
        assertEquals(1, select(
            listOf(campaign(1)), nowMillis = utcInstantAt(12, 0), quietHours = window
        )?.campaignId)
    }

    /**
     * Suppression costs the occurrence, not the campaign — nothing is recorded, so the same
     * campaign is eligible again next session with no manual reset.
     */
    @Test
    fun `suppression leaves the campaign eligible once the window ends`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        val c = campaign(1)
        assertNull(select(listOf(c), nowMillis = utcInstantAt(23, 0), quietHours = window))
        assertEquals(1, select(
            listOf(c), nowMillis = utcInstantAt(9, 0), quietHours = window
        )?.campaignId)
    }

    // --- the retry predicate ---

    /**
     * Defect 10: the retry path asked "has this ever displayed" rather than "may it show now",
     * and threw away every repeatable campaign that happened to be waiting.
     */
    @Test
    fun `mayDisplayNow keeps a deferred repeatable campaign that has already displayed`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = 60
        ))
        val history = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 120_000L, 3)), now - 120_000L
        )
        assertTrue(MessageSelector.mayDisplayNow(c, history, now, 30, null) { true })
    }

    @Test
    fun `mayDisplayNow re-checks the floor, so a message deferred before another displayed waits`() {
        val c = campaign(1)
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 5_000L)
        assertFalse(MessageSelector.mayDisplayNow(c, history, now, 30, null) { true })
    }

    @Test
    fun `mayDisplayNow refuses an expired, unsupported or artwork-less campaign`() {
        val history = DisplayHistorySnapshot()
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1, expiresAtMillis = now - 1), history, now, 0, null) { true })
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1, type = MessageType.UNSUPPORTED), history, now, 0, null) { true })
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1), history, now, 0, null) { false })
    }
}
