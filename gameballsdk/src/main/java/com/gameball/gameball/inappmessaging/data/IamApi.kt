package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.BuildConfig
import com.gameball.gameball.network.Config
import com.gameball.gameball.network.Network
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 1 = iOS, 2 = Android.
 *
 * Optional in the schema and load-bearing in fact: omit it, or send anything else, and the
 * backend returns 200 with an empty message list. Not an error — so a wrong platform code
 * produces a feature that silently does nothing and looks exactly like an account with no
 * campaigns.
 */
internal const val PLATFORM_ANDROID = 2

internal interface IamApi {

    /** Returns the raw body: [MessageParser] hand-walks it rather than binding reflectively. */
    @POST(Config.InAppMessagesSync)
    suspend fun sync(@Body body: SyncRequest): Response<ResponseBody>

    /**
     * Returns the raw body deliberately. Letting the converter bind it would make an
     * unparseable 2xx throw, and the batch would then be retried forever even though the
     * backend accepted it.
     */
    @POST(Config.InAppMessagesEvents)
    suspend fun sendEvents(@Body body: EventBatchRequest): Response<ResponseBody>

    @POST(Config.InAppMessagesVariables)
    suspend fun variables(@Body body: VariablesRequest): Response<VariablesResponse>

    companion object {
        /**
         * Built over the SDK's shared OkHttp client, so APIKey / Lang / x-gb-agent and the
         * v4.0 pin all apply without duplication, and over whatever base URL the host
         * configured — the iOS port shipped a fix for exactly the divergence that arises from
         * a module building its own.
         */
        fun create(apiPrefix: String?): IamApi = Retrofit.Builder()
            .baseUrl(apiPrefix ?: BuildConfig.API_Url)
            .client(Network.getInstance().okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Network.getInstance().gson))
            .build()
            .create(IamApi::class.java)
    }
}

internal data class SyncRequest(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("platform") val platform: Int,
    @SerializedName("locale") val locale: String,
    @SerializedName("appVersion") val appVersion: String?,
    @SerializedName("sdkVersion") val sdkVersion: String
)

internal data class EventBatchRequest(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("platform") val platform: Int,
    @SerializedName("events") val events: List<IamEventDto>
)

/** Nullable fields are omitted when null — Gson drops nulls by default. */
internal data class IamEventDto(
    @SerializedName("eventUid") val eventUid: String,
    @SerializedName("dispatchId") val dispatchId: String?,
    @SerializedName("campaignId") val campaignId: Int,
    @SerializedName("variationId") val variationId: Int?,
    @SerializedName("type") val type: String,
    @SerializedName("occurredAt") val occurredAt: String,
    @SerializedName("buttonId") val buttonId: String?,
    @SerializedName("url") val url: String?
)

internal data class EventBatchResponse(
    @SerializedName("accepted") val accepted: Int?,
    @SerializedName("rejected") val rejected: Int?
)

internal data class VariablesRequest(
    @SerializedName("customerId") val customerId: String
)

internal data class VariablesResponse(
    @SerializedName("variables") val variables: Map<String, String>?
)
