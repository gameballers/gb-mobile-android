package com.gameball.gameball.inappmessaging.artwork

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 */
internal interface ArtworkPrefetcher {
    suspend fun warm(urls: Set<String>)

    /** Null or blank counts as ready - a campaign with no artwork has nothing to wait for. */
    fun isReady(url: String?): Boolean

    /** Re-attempts only the failed set, at most once per 30s. Does not block the caller. */
    fun retryFailedIfDue(nowMillis: Long)

    fun reset()
}

/** Both slots must be ready before a campaign is eligible. */
internal fun ArtworkPrefetcher.isReady(content: MessageContent): Boolean =
    isReady(content.imageUrl) && isReady(content.iconUrl)

internal class PicassoArtworkPrefetcher(
    private val scope: CoroutineScope,
    @VisibleForTesting internal val fetch: suspend (String) -> Boolean = ::picassoFetch
) : ArtworkPrefetcher {

    private companion object {
        /**
         * Concurrent, so the ceiling is the slowest single image regardless of how many
         * campaigns arrive rather than the sum. Picasso has no per-request timeout.
         */
        const val WARM_TIMEOUT_MS = 5_000L
        const val RETRY_INTERVAL_MS = 30_000L
    }

    private val ready = LinkedHashSet<String>()
    private val failed = LinkedHashSet<String>()
    private var lastRetryAtMillis = 0L

    override suspend fun warm(urls: Set<String>) {
        val pending = synchronized(ready) {
            urls.filter { it.isNotBlank() && it !in ready }.toSet()
        }
        if (pending.isEmpty()) return

        pending.filter { it.startsWith("http://") }.forEach {
            // Cleartext is blocked by default since API 28, so the load fails and the only
            // symptom is a campaign that silently never shows. Name it in the log.
            IamLog.w(
                "artwork is served over cleartext http and will fail on API 28+: $it - the " +
                    "campaign using it will never display"
            )
        }

        val results = withTimeoutOrNull(WARM_TIMEOUT_MS) {
            coroutineScope {
                pending.map { url -> async { url to runCatching { fetch(url) }.getOrDefault(false) } }
                    .awaitAll()
            }
        }

        synchronized(ready) {
            if (results == null) {
                // The whole set timed out; whatever has not already succeeded is unready.
                pending.forEach { if (it !in ready) failed.add(it) }
                IamLog.w("artwork warming exceeded ${WARM_TIMEOUT_MS}ms; ${failed.size} unready")
            } else {
                results.forEach { (url, ok) ->
                    if (ok) { ready.add(url); failed.remove(url) } else failed.add(url)
                }
            }
        }
    }

    override fun isReady(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return synchronized(ready) { url in ready }
    }

    /**
     * The failure verdict is not cached for the session. Flutter computed it once at sync,
     * and a two-second blip made a campaign undisplayable for eight minutes and every trigger
     * in between. The gate asks a display-time question, so answering it once at sync makes
     * it a question about the past.
     */
    override fun retryFailedIfDue(nowMillis: Long) {
        val toRetry = synchronized(ready) {
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
        synchronized(ready) {
            ready.clear()
            failed.clear()
            lastRetryAtMillis = 0L
        }
    }

    @VisibleForTesting
    internal fun failedCount(): Int = synchronized(ready) { failed.size }
}

/**
 * Picasso's fetch() warms the cache with no target view, which is exactly the primitive a
 * prefetcher needs; at display an ordinary load().into() hits the warm cache.
 */
private suspend fun picassoFetch(url: String): Boolean = suspendCancellableCoroutine { cont ->
    try {
        Picasso.get().load(url).fetch(object : Callback {
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
