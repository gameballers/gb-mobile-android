package com.gameball.gameball.inappmessaging.data

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal interface VariableSource {
    /**
     * Never throws and never blocks longer than its own bound. Returns the best values it
     * has: fresh, cached, persisted, or empty.
     */
    suspend fun values(customerId: String, needed: Set<String>): Map<String, String>

    /** Drops the freshness cache but keeps the persisted fallback. */
    fun invalidate()

    /** Removes everything, storage included. Logout and customer change. */
    fun clear()
}

internal class RemoteVariableSource(
    private val api: IamApi,
    private val store: IamStore,
    private val scope: CoroutineScope,
    private val clock: Clock
) : VariableSource {

    private companion object {
        /**
         * The budget was never the real problem in Flutter - the round trip measured 716ms.
         * What made a slow request into visible breakage was the fallback being unreachable.
         */
        const val FETCH_TIMEOUT_MS = 2_000L
        const val CACHE_TTL_MS = 60_000L
    }

    private var cached: Map<String, String>? = null
    private var cachedAtMillis: Long = 0
    private var cachedCustomerId: String? = null

    @Volatile
    private var currentCustomerId: String? = null

    override suspend fun values(customerId: String, needed: Set<String>): Map<String, String> {
        currentCustomerId = customerId

        val fresh = cached
        if (fresh != null &&
            cachedCustomerId == customerId &&
            clock.nowMillis() - cachedAtMillis < CACHE_TTL_MS
        ) {
            return fresh
        }

        // The timeout lives here, inside the source that owns the fallback. In Flutter it
        // fired in the service instead, outside the fallback's reach, and a 716ms call
        // rendered a raw {player_name} on screen.
        val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            withContext(Dispatchers.IO) { fetch(customerId) }
        }

        if (fetched == null || fetched.isEmpty()) {
            IamLog.d("variables unavailable; falling back to the values already held")
            return cached?.takeIf { cachedCustomerId == customerId }
                ?: readPersisted(customerId)
        }

        cached = fetched
        cachedAtMillis = clock.nowMillis()
        cachedCustomerId = customerId
        persistFiltered(customerId, fetched, needed)
        return fetched
    }

    private suspend fun fetch(customerId: String): Map<String, String> = try {
        val response = api.variables(VariablesRequest(customerId))
        if (response.isSuccessful) {
            response.body()?.variables.orEmpty()
        } else {
            // 404, 422, 503 all deserve the same response from the caller: keep what you hold.
            IamLog.d("variables endpoint answered HTTP ${response.code()}")
            emptyMap()
        }
    } catch (t: Throwable) {
        IamLog.d("variables endpoint unreachable: ${t.message}")
        emptyMap()
    }

    /**
     * The endpoint returns all nine keys, four of which are personal data, whether or not any
     * campaign mentions them. Only the tokens the held campaigns actually use land on disk;
     * a campaign set mentioning none stores nothing at all.
     */
    private fun persistFiltered(
        customerId: String,
        fetched: Map<String, String>,
        needed: Set<String>
    ) {
        val toPersist = fetched.filterKeys { it in needed }
        if (toPersist.isEmpty()) {
            store.clear(IamStore.Slot.VARIABLES)
            return
        }
        // Not awaited: a display must never wait on storage. That opens a race with clear(),
        // closed by re-checking the customer AFTER the suspension point below.
        scope.launch {
            // A check placed before the suspension point always passes, because the clear has
            // not been issued yet. This one is after it, so a clear cannot be overtaken.
            if (currentCustomerId != customerId) {
                IamLog.d("customer changed while persisting variables; discarding the write")
                return@launch
            }
            val json = JsonObject()
            toPersist.forEach { (key, value) -> json.addProperty(key, value) }
            store.writeScoped(IamStore.Slot.VARIABLES, customerId, json.toString())
        }
    }

    @VisibleForTesting
    internal fun readPersisted(customerId: String): Map<String, String> {
        val raw = store.readScoped(IamStore.Slot.VARIABLES, customerId) ?: return emptyMap()
        return try {
            @Suppress("DEPRECATION")
            val obj = JsonParser().parse(raw).takeIf { it.isJsonObject }?.asJsonObject
                ?: return emptyMap()
            obj.entrySet().mapNotNull { (key, value) ->
                value.scalar()?.let { key to it.toString() }
            }.toMap()
        } catch (t: Throwable) {
            IamLog.w("stored variables are unreadable; discarding them")
            store.clear(IamStore.Slot.VARIABLES)
            emptyMap()
        }
    }

    /**
     * Dropped on every event and purchase, before evaluating - the campaign this exists for
     * is "you just earned 200 points, you now have X", whose trigger is the purchase, so a
     * value cached before it quotes the number from before the change.
     *
     * The persisted copy is deliberately kept: clearing it here is what made the fallback
     * unreachable in Flutter.
     */
    override fun invalidate() {
        cached = null
        cachedAtMillis = 0
        cachedCustomerId = null
    }

    override fun clear() {
        invalidate()
        currentCustomerId = null
        store.clear(IamStore.Slot.VARIABLES)
    }
}
