package com.gameball.gameball.inappmessaging.artwork

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.squareup.picasso.Callback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Warms every held campaign's artwork at sync, before anything displays.
 *
 * A campaign whose artwork failed is passed over at selection, letting a lower-priority ready
 * one take the slot. Both major competitors agree - Braze: "Failed image downloads prevent
 * in-app messages with images from displaying." Do not "improve" this by rendering text-only.
 *
 * Why it matters: the impression fires when the view becomes visible, so artwork arriving a
 * beat later means a view was counted of something the customer could not see.
 *
 * Slow is not failed. The batch warm has a budget that bounds how long the SDK waits before
 * evaluating the first trigger, but a URL that has not landed by then continues fetching in
 * the background and moves to READY or FAILED on its own verdict. The selector treats FAILED
 * as ineligible; the service treats LOADING as "wait a short grace for it before falling
 * through to a lower priority", so a slow CDN never silently inverts a marketer's priority.
 */
internal interface ArtworkPrefetcher {
    suspend fun warm(urls: Set<String>)

    /** Null or blank counts as ready - a campaign with no artwork has nothing to wait for. */
    fun isReady(url: String?): Boolean = stateOf(url) == ArtworkState.READY

    fun stateOf(url: String?): ArtworkState

    /**
     * Suspend until [url] leaves the LOADING state or [timeoutMs] elapses. Returns true iff
     * the URL is READY on wake. A URL that was not in LOADING to begin with resolves against
     * its current state without waiting.
     */
    suspend fun awaitReady(url: String, timeoutMs: Long): Boolean

    /** Re-attempts only the failed set, at most once per 30s. Does not block the caller. */
    fun retryFailedIfDue(nowMillis: Long)

    fun reset()
}

internal enum class ArtworkState { READY, LOADING, FAILED }

/** Both slots must be ready before a campaign is eligible. */
internal fun ArtworkPrefetcher.isReady(content: MessageContent): Boolean =
    stateOf(content) == ArtworkState.READY

/**
 * FAILED dominates LOADING dominates READY: a campaign with a failed image is ineligible
 * regardless of its icon; a loading image still gives it a grace at display time.
 */
internal fun ArtworkPrefetcher.stateOf(content: MessageContent): ArtworkState {
    val image = stateOf(content.imageUrl)
    val icon = stateOf(content.iconUrl)
    return when {
        image == ArtworkState.FAILED || icon == ArtworkState.FAILED -> ArtworkState.FAILED
        image == ArtworkState.LOADING || icon == ArtworkState.LOADING -> ArtworkState.LOADING
        else -> ArtworkState.READY
    }
}

internal class PicassoArtworkPrefetcher(
    private val scope: CoroutineScope,
    @VisibleForTesting internal val fetch: suspend (String) -> Boolean = ::picassoFetch
) : ArtworkPrefetcher {

    private companion object {
        /**
         * The ceiling on the initial batch wait, not on any single fetch. Fetches that
         * outlast the wait continue in the background and land into READY or FAILED on their
         * own verdict; the wait exists so the first trigger after sync is not held forever.
         */
        const val WARM_TIMEOUT_MS = 5_000L
        const val RETRY_INTERVAL_MS = 30_000L
    }

    private val lock = Any()
    private val ready = LinkedHashSet<String>()
    private val failed = LinkedHashSet<String>()
    private val loading = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private var lastRetryAtMillis = 0L

    override suspend fun warm(urls: Set<String>) {
        val toStart = mutableMapOf<String, CompletableDeferred<Boolean>>()
        val toAwait = mutableListOf<CompletableDeferred<Boolean>>()
        synchronized(lock) {
            urls.filter { it.isNotBlank() }.forEach { url ->
                when {
                    url in ready -> Unit
                    loading[url] != null -> toAwait += loading.getValue(url)
                    else -> {
                        val d = CompletableDeferred<Boolean>()
                        loading[url] = d
                        toStart[url] = d
                        toAwait += d
                    }
                }
            }
        }
        if (toAwait.isEmpty()) return

        toStart.keys.filter { it.startsWith("http://") }.forEach {
            // Cleartext is blocked by default since API 28, so the load fails and the only
            // symptom is a campaign that silently never shows. Name it in the log.
            IamLog.w(
                "artwork is served over cleartext http and will fail on API 28+: $it - the " +
                    "campaign using it will never display"
            )
        }

        // The fetches run on the module scope, not the caller's, so the WARM_TIMEOUT_MS below
        // bounds only the wait. A URL that lands after the deadline still moves to READY or
        // FAILED on its own timeline, and the next evaluation picks it up correctly.
        toStart.forEach { (url, deferred) ->
            scope.launch {
                val ok = runCatching { fetch(url) }.getOrDefault(false)
                synchronized(lock) {
                    loading.remove(url)
                    if (ok) { ready.add(url); failed.remove(url) } else failed.add(url)
                }
                deferred.complete(ok)
            }
        }

        withTimeoutOrNull(WARM_TIMEOUT_MS) {
            toAwait.forEach { runCatching { it.await() } }
        }

        val stillLoading = synchronized(lock) { toStart.keys.count { it in loading } }
        if (stillLoading > 0) {
            IamLog.w(
                "artwork warming exceeded ${WARM_TIMEOUT_MS}ms; $stillLoading still loading " +
                    "in the background"
            )
        }
    }

    override fun stateOf(url: String?): ArtworkState {
        if (url.isNullOrBlank()) return ArtworkState.READY
        return synchronized(lock) {
            when {
                url in ready -> ArtworkState.READY
                url in loading -> ArtworkState.LOADING
                url in failed -> ArtworkState.FAILED
                else -> ArtworkState.FAILED
            }
        }
    }

    override suspend fun awaitReady(url: String, timeoutMs: Long): Boolean {
        if (url.isBlank()) return true
        val deferred = synchronized(lock) { loading[url] } ?: return isReady(url)
        val landed = withTimeoutOrNull(timeoutMs) {
            runCatching { deferred.await() }.getOrDefault(false)
        }
        return landed == true
    }

    /**
     * The failure verdict is not cached for the session. Flutter computed it once at sync,
     * and a two-second blip made a campaign undisplayable for eight minutes and every trigger
     * in between. The gate asks a display-time question, so answering it once at sync makes
     * it a question about the past.
     */
    override fun retryFailedIfDue(nowMillis: Long) {
        val toRetry = synchronized(lock) {
            if (failed.isEmpty()) return
            if (nowMillis - lastRetryAtMillis < RETRY_INTERVAL_MS) return
            lastRetryAtMillis = nowMillis
            // Only the failed set: re-fetching decoded artwork costs a request per campaign
            // per retry for nothing.
            failed.toSet()
        }
        IamLog.d("re-attempting ${toRetry.size} failed artwork url(s)")
        // Fired without blocking the evaluation in flight: the current trigger still misses,
        // every later one recovers.
        scope.launch { warm(toRetry) }
    }

    override fun reset() {
        synchronized(lock) {
            ready.clear()
            failed.clear()
            loading.values.forEach { it.complete(false) }
            loading.clear()
            lastRetryAtMillis = 0L
        }
    }

    @VisibleForTesting
    internal fun failedCount(): Int = synchronized(lock) { failed.size }
}

/**
 * Picasso's fetch() warms the cache with no target view, which is exactly the primitive a
 * prefetcher needs; at display an ordinary load().into() hits the warm cache.
 */
private suspend fun picassoFetch(url: String): Boolean = suspendCancellableCoroutine { cont ->
    try {
        IamImageLoader.fetch(url, object : Callback {
            override fun onSuccess() { if (cont.isActive) cont.resume(true) }
            override fun onError(e: Exception?) {
                IamLog.d("artwork failed to load: $url (${e?.message})")
                if (cont.isActive) cont.resume(false)
            }
        })
    } catch (t: Throwable) {
        if (cont.isActive) cont.resume(false)
    }
}
