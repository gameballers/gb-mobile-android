package com.gameball.gameball.inappmessaging.artwork

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class ArtworkPrefetcherTest {

    private val attempts: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun prefetcher(
        fetch: suspend (String) -> Boolean = { true }
    ) = PicassoArtworkPrefetcher(CoroutineScope(Dispatchers.Unconfined)) { url ->
        attempts.add(url)
        fetch(url)
    }

    @Test
    fun `a null or blank url is ready because there is nothing to wait for`() {
        val p = prefetcher()
        assertTrue(p.isReady(null))
        assertTrue(p.isReady(""))
        assertTrue(p.isReady("   "))
    }

    @Test
    fun `a warmed url is ready and a failed one is not`() {
        val p = prefetcher { url -> url.contains("good") }
        runBlocking { p.warm(setOf("https://x/good.jpg", "https://x/bad.jpg")) }
        assertTrue(p.isReady("https://x/good.jpg"))
        assertFalse(p.isReady("https://x/bad.jpg"))
    }

    @Test
    fun `an unwarmed url is not ready`() {
        assertFalse(prefetcher().isReady("https://x/never-seen.jpg"))
    }

    /** The ceiling is the slowest single image, not the sum of all of them. */
    @Test
    fun `the whole set is warmed concurrently`() {
        val p = prefetcher { delay(300); true }
        val elapsed = runBlocking {
            val start = System.nanoTime()
            p.warm((1..8).map { "https://x/$it.jpg" }.toSet())
            (System.nanoTime() - start) / 1_000_000
        }
        assertTrue("8 x 300ms serially would be 2400ms; took ${elapsed}ms", elapsed < 1_500)
        assertTrue(p.isReady("https://x/1.jpg"))
    }

    @Test
    fun `a hung load is bounded and the campaign is left unready`() {
        val p = prefetcher { delay(60_000); true }
        val elapsed = runBlocking {
            val start = System.nanoTime()
            p.warm(setOf("https://x/hangs.jpg"))
            (System.nanoTime() - start) / 1_000_000
        }
        assertTrue("should give up near 5s, took ${elapsed}ms", elapsed in 4_000..8_000)
        assertFalse(p.isReady("https://x/hangs.jpg"))
    }

    @Test
    fun `a throwing fetch is treated as a failure rather than propagating`() {
        val p = prefetcher { error("boom") }
        runBlocking { p.warm(setOf("https://x/throws.jpg")) }
        assertFalse(p.isReady("https://x/throws.jpg"))
    }

    /**
     * Defect 11: Flutter cached the verdict for the session, so a two-second blip made a
     * campaign undisplayable for eight minutes.
     */
    @Test
    fun `the failed set is re-attempted after thirty seconds and recovers`() {
        var healthy = false
        val p = prefetcher { healthy }
        runBlocking { p.warm(setOf("https://x/flaky.jpg")) }
        assertFalse(p.isReady("https://x/flaky.jpg"))

        healthy = true
        p.retryFailedIfDue(30_000L)
        assertTrue("the campaign must become displayable again", p.isReady("https://x/flaky.jpg"))
    }

    @Test
    fun `it does not re-attempt within thirty seconds`() {
        val p = prefetcher { false }
        runBlocking { p.warm(setOf("https://x/flaky.jpg")) }
        val afterWarm = attempts.size

        p.retryFailedIfDue(1_000L)
        p.retryFailedIfDue(29_999L)
        assertEquals("too soon to retry", afterWarm, attempts.size)

        p.retryFailedIfDue(30_000L)
        assertEquals(afterWarm + 1, attempts.size)
    }

    /** Re-fetching decoded artwork costs a request per campaign per retry for nothing. */
    @Test
    fun `the retry asks only about what failed`() {
        val p = prefetcher { url -> url.contains("good") }
        runBlocking { p.warm(setOf("https://x/good.jpg", "https://x/bad.jpg")) }
        attempts.clear()

        p.retryFailedIfDue(30_000L)
        assertEquals(listOf("https://x/bad.jpg"), attempts.toList())
    }

    @Test
    fun `nothing failed means nothing is retried`() {
        val p = prefetcher { true }
        runBlocking { p.warm(setOf("https://x/good.jpg")) }
        attempts.clear()
        p.retryFailedIfDue(30_000L)
        assertTrue(attempts.isEmpty())
    }

    @Test
    fun `an already-ready url is not re-fetched`() {
        val p = prefetcher { true }
        runBlocking { p.warm(setOf("https://x/a.jpg")) }
        attempts.clear()
        runBlocking { p.warm(setOf("https://x/a.jpg")) }
        assertTrue(attempts.isEmpty())
    }

    @Test
    fun `reset clears both the ready and the failed sets`() {
        val p = prefetcher { url -> url.contains("good") }
        runBlocking { p.warm(setOf("https://x/good.jpg", "https://x/bad.jpg")) }
        assertEquals(1, p.failedCount())

        p.reset()
        assertFalse(p.isReady("https://x/good.jpg"))
        assertEquals(0, p.failedCount())
    }

    // --- tri-state readiness ---

    /**
     * A URL that outlasts the warm budget is LOADING, not FAILED - so a slow CDN cannot
     * silently invert a marketer's priority in the selector (defect 7, 30 Aug 2026).
     */
    @Test
    fun `a url that outlasts the warm budget stays in the loading state`() {
        val p = prefetcher { delay(60_000); true }
        runBlocking { p.warm(setOf("https://x/slow.jpg")) }
        assertEquals(ArtworkState.LOADING, p.stateOf("https://x/slow.jpg"))
    }

    @Test
    fun `a failed fetch moves the url to the failed state`() {
        val p = prefetcher { false }
        runBlocking { p.warm(setOf("https://x/dead.jpg")) }
        assertEquals(ArtworkState.FAILED, p.stateOf("https://x/dead.jpg"))
    }

    @Test
    fun `a warmed url is in the ready state`() {
        val p = prefetcher { true }
        runBlocking { p.warm(setOf("https://x/ok.jpg")) }
        assertEquals(ArtworkState.READY, p.stateOf("https://x/ok.jpg"))
    }

    // --- awaitReady ---

    @Test
    fun `awaitReady resolves true when a loading url lands within the budget`() {
        val p = prefetcher { delay(200); true }
        runBlocking {
            // Kick off the warm but do not await it - the fetch is still in flight when
            // awaitReady is called.
            val warmJob = launch { p.warm(setOf("https://x/slow.jpg")) }
            // Small yield so warm registers the loading entry before we ask.
            delay(20)
            val landed = p.awaitReady("https://x/slow.jpg", 1_000)
            assertTrue("awaitReady must return true once the url lands", landed)
            warmJob.join()
        }
    }

    @Test
    fun `awaitReady resolves false when the budget elapses before the url lands`() {
        val p = prefetcher { delay(60_000); true }
        runBlocking { p.warm(setOf("https://x/slow.jpg")) }
        val landed = runBlocking { p.awaitReady("https://x/slow.jpg", 100) }
        assertFalse("awaitReady must return false when the wait times out", landed)
    }

    @Test
    fun `awaitReady on an already-ready url returns immediately`() {
        val p = prefetcher { true }
        runBlocking { p.warm(setOf("https://x/ok.jpg")) }
        val landed = runBlocking { p.awaitReady("https://x/ok.jpg", 0) }
        assertTrue(landed)
    }
}
