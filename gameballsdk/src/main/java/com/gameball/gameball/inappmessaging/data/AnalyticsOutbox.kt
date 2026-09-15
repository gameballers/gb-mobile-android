package com.gameball.gameball.inappmessaging.data

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.UUID

internal enum class IamEventType(val wire: String) {
    IMPRESSION("impression"),
    CLICK("click"),
    DISMISS("dismiss")
}

/**
 * There is no separate button-click type: a button tap is a [IamEventType.CLICK] carrying
 * [buttonId], and the presence of that field is what distinguishes it from a tap on the
 * message surface.
 */
internal data class IamEvent(
    /**
     * A real UUID v4, generated once and never regenerated on retry — that is the entire
     * point of the field. A non-GUID is a hard 400 that discards the whole batch, and
     * server-side dedup on this value is what makes at-least-once delivery safe.
     */
    val eventUid: String,
    val customerId: String,
    val dispatchId: String?,
    val campaignId: Int,
    val variationId: Int?,
    val type: IamEventType,
    /** When it happened on the device, never when it was sent. */
    val occurredAtMillis: Long,
    val buttonId: String? = null,
    val url: String? = null
) {
    companion object {
        fun newUid(): String = UUID.randomUUID().toString()
    }
}

internal enum class FlushOutcome { ACCEPTED, RETRY, DISCARD }

/**
 * The endpoint answers 202, not 200. Narrowing success to exactly 200 reported every accepted
 * event to the host as a failure.
 *
 * Every 4xx outside 408 and 429 discards rather than retrying. The spec names 400/401/404/422;
 * mapping the rest the same way is deliberate, because the outbox is FIFO and one permanently
 * rejected batch at the head blocks every event behind it and takes all analytics down.
 */
internal fun outcomeFor(code: Int): FlushOutcome = when {
    code in 200..299 -> FlushOutcome.ACCEPTED
    code == 408 || code == 429 -> FlushOutcome.RETRY
    code in 400..499 -> FlushOutcome.DISCARD
    else -> FlushOutcome.RETRY
}

internal interface MessageAnalytics {
    fun start()
    /** Never blocks the caller: an impression must not wait on a round trip. */
    fun record(event: IamEvent)
    suspend fun flush()
    fun dispose()
}

internal class PersistentAnalyticsOutbox(
    private val api: IamApi,
    private val store: IamStore,
    private val scope: CoroutineScope,
    private val clock: Clock
) : MessageAnalytics {

    private companion object {
        const val FLUSH_INTERVAL_MS = 30_000L
        const val FLUSH_AT_COUNT = 10
        const val EVENTS_PER_REQUEST = 50
        const val CEILING = 500
    }

    private val queue = ArrayList<IamEvent>()
    private val lock = Any()
    private val inFlight = Mutex()
    private var scheduler: Job? = null

    @Volatile
    private var disposed = false

    override fun start() {
        // Disposing sets a "do not schedule" flag; without clearing it here, after one
        // stop/start cycle the timer is never armed again and events only go out when the
        // batch happens to reach its count threshold.
        disposed = false
        load()
        armScheduler()
    }

    private fun armScheduler() {
        if (disposed || scheduler?.isActive == true) return
        scheduler = scope.launch {
            while (isActive && !disposed) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    override fun record(event: IamEvent) {
        val shouldFlush: Boolean
        synchronized(lock) {
            queue.add(event)
            if (queue.size > CEILING) {
                val dropped = queue.size - CEILING
                repeat(dropped) { queue.removeAt(0) }
                IamLog.w("analytics outbox hit its $CEILING ceiling; dropped $dropped oldest event(s)")
            }
            shouldFlush = queue.size >= FLUSH_AT_COUNT
        }
        persist()
        if (shouldFlush) scope.launch { flush() }
    }

    /**
     * One request in flight at a time: re-entering would double-send a batch, and the
     * in-flight one re-arms on completion anyway.
     */
    override suspend fun flush() {
        if (!inFlight.tryLock()) return
        try {
            while (true) {
                val batch = synchronized(lock) { queue.take(EVENTS_PER_REQUEST) }
                if (batch.isEmpty()) return

                // Grouped by customer: the batch body names one customerId at its root, and
                // events must never be attributed to whoever happens to be signed in now.
                val byCustomer = batch.groupBy { it.customerId }
                var anyRetry = false
                for ((customerId, events) in byCustomer) {
                    when (send(customerId, events)) {
                        FlushOutcome.ACCEPTED, FlushOutcome.DISCARD ->
                            synchronized(lock) { queue.removeAll(events.toSet()) }
                        FlushOutcome.RETRY -> anyRetry = true
                    }
                }
                persist()
                // No backoff: a retryable failure simply waits for the ordinary timer, and a
                // successful flush that leaves a backlog re-flushes immediately rather than
                // taking minutes to drain.
                if (anyRetry) return
                if (synchronized(lock) { queue.isEmpty() }) return
            }
        } finally {
            inFlight.unlock()
        }
    }

    private suspend fun send(customerId: String, events: List<IamEvent>): FlushOutcome =
        withContext(Dispatchers.IO) {
            try {
                val response = api.sendEvents(
                    EventBatchRequest(
                        customerId = customerId,
                        platform = PLATFORM_ANDROID,
                        events = events.map { it.toDto() }
                    )
                )
                val outcome = outcomeFor(response.code())
                if (outcome == FlushOutcome.ACCEPTED) {
                    // An unreadable 2xx body is still accepted; the counts are only
                    // diagnostics, so they are read defensively and never gate the outcome.
                    val rejected = runCatching {
                        val raw = response.body()?.string().orEmpty()
                        @Suppress("DEPRECATION")
                        JsonParser().parse(raw).takeIf { it.isJsonObject }?.asJsonObject
                            ?.int("rejected")
                    }.getOrNull()
                    if (rejected != null && rejected > 0) {
                        IamLog.w(
                            "$rejected of ${events.size} analytics events were rejected as " +
                                "malformed; they can never succeed and are not retried"
                        )
                    }
                } else {
                    IamLog.w("analytics flush got HTTP ${response.code()} -> $outcome")
                }
                outcome
            } catch (t: Throwable) {
                IamLog.w("analytics flush could not reach the backend: ${t.message}")
                FlushOutcome.RETRY
            }
        }

    override fun dispose() {
        disposed = true
        scheduler?.cancel()
        scheduler = null
    }

    // --- persistence: after every change, so an impression logged a second before a
    // force-quit still arrives on the next launch ---

    private fun persist() {
        val snapshot = synchronized(lock) { queue.toList() }
        val array = JsonArray()
        snapshot.forEach { array.add(it.toJson()) }
        store.writeRaw(IamStore.Slot.OUTBOX, array.toString())
    }

    @VisibleForTesting
    internal fun load() {
        val raw = store.readRaw(IamStore.Slot.OUTBOX) ?: return
        try {
            @Suppress("DEPRECATION")
            val array = JsonParser().parse(raw).takeIf { it.isJsonArray }?.asJsonArray ?: return
            val restored = ArrayList<IamEvent>(array.size())
            array.forEach { element ->
                element.takeIf { it.isJsonObject }?.asJsonObject?.toEvent()?.let(restored::add)
            }
            synchronized(lock) {
                queue.clear()
                queue.addAll(restored)
            }
            if (restored.isNotEmpty()) IamLog.d("restored ${restored.size} queued analytics event(s)")
        } catch (t: Throwable) {
            IamLog.w("analytics outbox is unreadable; discarding it")
            store.clear(IamStore.Slot.OUTBOX)
        }
    }

    @VisibleForTesting
    internal fun queuedCount(): Int = synchronized(lock) { queue.size }

    /** Seeds the persisted queue without triggering the count-threshold flush. Tests only. */
    @VisibleForTesting
    internal fun recordWithoutFlush(event: IamEvent) {
        synchronized(lock) { queue.add(event) }
        persist()
    }

    private fun IamEvent.toDto() = IamEventDto(
        eventUid = eventUid,
        dispatchId = dispatchId,
        campaignId = campaignId,
        variationId = variationId,
        type = type.wire,
        occurredAt = IamTime.toIso8601Utc(occurredAtMillis),
        buttonId = buttonId,
        url = url
    )

    private fun IamEvent.toJson() = JsonObject().apply {
        addProperty("eventUid", eventUid)
        addProperty("customerId", customerId)
        dispatchId?.let { addProperty("dispatchId", it) }
        addProperty("campaignId", campaignId)
        variationId?.let { addProperty("variationId", it) }
        addProperty("type", type.wire)
        addProperty("occurredAt", occurredAtMillis)
        buttonId?.let { addProperty("buttonId", it) }
        url?.let { addProperty("url", it) }
    }

    private fun JsonObject.toEvent(): IamEvent? {
        val uid = str("eventUid") ?: return null
        val customerId = str("customerId") ?: return null
        val campaignId = int("campaignId") ?: return null
        val type = IamEventType.values().firstOrNull { it.wire == str("type") } ?: return null
        val occurredAt = long("occurredAt") ?: return null
        return IamEvent(
            eventUid = uid,
            customerId = customerId,
            dispatchId = str("dispatchId"),
            campaignId = campaignId,
            variationId = int("variationId"),
            type = type,
            occurredAtMillis = occurredAt,
            buttonId = str("buttonId"),
            url = str("url")
        )
    }
}
