package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.SyncResult
import com.gameball.gameball.inappmessaging.runtime.IamLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed class SyncOutcome {
    /** Carries the raw payload so the caller can cache it verbatim. */
    data class Success(val rawPayload: String, val result: SyncResult) : SyncOutcome()

    /**
     * Every non-2xx means "could not ask", which is not the same as "no campaigns" — only a
     * failure falls back to the cache. [permanent] separates "stop expecting this to work" from
     * "try again next session".
     */
    data class Failure(val reason: String, val permanent: Boolean) : SyncOutcome()
}

internal interface MessageSource {
    suspend fun fetch(customerId: String): SyncOutcome
}

internal class RemoteMessageSource(
    private val api: IamApi,
    private val localeProvider: () -> String,
    private val appVersion: String?,
    private val sdkVersion: String
) : MessageSource {

    init {
        if (PLATFORM_ANDROID != 1 && PLATFORM_ANDROID != 2) {
            IamLog.e(
                "platform code $PLATFORM_ANDROID is not 1 or 2; the backend will answer 200 " +
                    "with an empty message list and messaging will silently do nothing"
            )
        }
    }

    override suspend fun fetch(customerId: String): SyncOutcome = withContext(Dispatchers.IO) {
        try {
            val response = api.sync(
                SyncRequest(
                    customerId = customerId,
                    platform = PLATFORM_ANDROID,
                    locale = localeProvider(),
                    appVersion = appVersion,
                    sdkVersion = sdkVersion
                )
            )
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                return@withContext failureFor(response.code(), errorBody.isNotBlank())
            }
            // A 200 whose body will not parse is still a served response, not a network
            // failure: the parser returns an empty result and the cache is replaced.
            val raw = response.body()?.string().orEmpty()
            SyncOutcome.Success(raw, MessageParser.parse(raw))
        } catch (t: Throwable) {
            IamLog.w("sync could not reach the backend: ${t.message}")
            SyncOutcome.Failure(t.message ?: "network error", permanent = false)
        }
    }

    private fun failureFor(code: Int, hasBody: Boolean): SyncOutcome.Failure = when (code) {
        400 -> SyncOutcome.Failure("400 - the sync request had no customerId", permanent = true)
        401 -> SyncOutcome.Failure("401 - the API key was rejected", permanent = true)
        // Two very different problems behind one status. Distinguishing them is the difference
        // between "this customer does not exist yet" and "you are pointed at the wrong host",
        // and conflating them makes both look like a misconfigured base URL.
        404 -> if (hasBody) {
            SyncOutcome.Failure(
                "404 - the backend does not know this customer yet", permanent = false
            )
        } else {
            SyncOutcome.Failure(
                "404 with no body - the in-app messaging endpoints are not deployed on this " +
                    "environment. The V4 endpoints are alpha-only; production returns a bare 404",
                permanent = true
            )
        }
        422 -> SyncOutcome.Failure("422 - the customer is deactivated", permanent = true)
        else -> SyncOutcome.Failure("HTTP $code", permanent = false)
    }
}
