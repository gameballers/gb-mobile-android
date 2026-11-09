package com.gameball.gameball.engagement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InfluencedOpensTest {

    private var clock = 1_000_000_000L
    private val minute = 60_000L

    private val store = object : ReceivedPushStore {
        var saved: List<ReceivedPush> = emptyList()
        override fun load() = saved
        override fun save(pushes: List<ReceivedPush>) { saved = pushes }
    }
    private val influenced = InfluencedOpens(store, now = { clock })

    private fun receive(token: String) = influenced.onReceived(token, token, sentAt = clock - 500)
    private fun open() = influenced.influencedBy(AppOpen(AppOpenObserver.TYPE_WARM, clock))

    @Test
    fun openWithinTheWindowOfAnUntappedPushIsInfluenced() {
        receive("tok-1")
        clock += 10 * minute
        assertEquals("tok-1", open()?.clickToken)
    }

    @Test
    fun openAfterTheWindowIsNotInfluenced() {
        receive("tok-1")
        clock += 31 * minute
        assertNull(open())
    }

    @Test
    fun tappedPushNeverCountsAsInfluenced() {
        receive("tok-1")
        clock += minute
        influenced.onTap("tok-1")
        assertNull(open())
    }

    @Test
    fun anOpenFromATapIsNeverInfluencedEvenWithAnOlderUntappedPush() {
        receive("tok-A")
        clock += 5 * minute
        receive("tok-B")
        influenced.onTap("tok-B")
        assertNull(influenced.influencedBy(AppOpen(AppOpenObserver.TYPE_WARM, clock, fromTap = true)))
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun theMostRecentPushGetsTheCredit() {
        receive("tok-1")
        clock += 5 * minute
        receive("tok-2")
        clock += 5 * minute
        assertEquals("tok-2", open()?.clickToken)
    }

    @Test
    fun anOpenClearsTheSavedPushes() {
        receive("tok-1")
        clock += minute
        open()
        clock += minute
        assertNull(open())
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun openWithNoPushIsNotInfluenced() {
        assertNull(open())
    }

    @Test
    fun theSamePushDeliveredTwiceIsSavedOnce() {
        receive("tok-1")
        receive("tok-1")
        assertEquals(1, store.saved.size)
    }

    @Test
    fun atMostTwentyPushesAreKept() {
        repeat(25) { receive("tok-$it"); clock += 1 }
        assertEquals(InfluencedOpens.MAX_SAVED, store.saved.size)
        assertEquals("tok-24", store.saved.last().clickToken)
    }

    @Test
    fun clearForgetsEverything() {
        receive("tok-1")
        influenced.clear()
        assertNull(open())
    }
}
