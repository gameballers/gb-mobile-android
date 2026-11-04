package com.gameball.gameball.inappmessaging.data

import android.net.Uri
import androidx.annotation.VisibleForTesting

/**
 * Composes the widget URL a game campaign opens.
 *
 * Built here from state the SDK already holds, never carried on the campaign. A URL on the wire
 * would have to include the api key and the customer id, making the payload both
 * credential-bearing and customer-specific, and stale the moment either changed.
 *
 * `hideNavigation` is always on: a campaign opens *one* game, and leaving the widget's own
 * navigation in place would turn an in-app message into a second entry point to the whole profile.
 */
internal object WidgetUrl {

    /**
     * @param host the widget host, without a scheme — `m.gameball.app`, or an alpha host when the
     *   integrator configured one. Resolved by the caller from SDK configuration so that a host
     *   aimed at a non-production environment is not aimed there for its profile widget and at
     *   production for a campaign a message opens.
     */
    fun build(
        host: String,
        apiKey: String?,
        customerId: String?,
        openDetail: String,
        language: String?,
        mainColor: String? = null,
        platform: String? = null,
        shop: String? = null,
        sdkVersion: String? = null,
        osVersion: String? = null,
        sessionToken: String? = null
    ): String {
        val uri = Uri.Builder()
            .scheme("https")
            .encodedAuthority(host)
            .appendEncodedPath("")

        language?.let { uri.appendQueryParameter("lang", it) }
        uri.appendQueryParameter("apiKey", apiKey ?: "")
        mainColor?.let { uri.appendQueryParameter("main", it.removePrefix("#")) }
        uri.appendQueryParameter("customerId", customerId ?: "")
        platform?.let { uri.appendQueryParameter("platform", it) }
        shop?.let { uri.appendQueryParameter("shop", it) }
        sdkVersion?.let { uri.appendQueryParameter("sdk", it) }
        osVersion?.let { uri.appendQueryParameter("os", it) }
        uri.appendQueryParameter("openDetail", openDetail)
        uri.appendQueryParameter("hideNavigation", HIDE_NAVIGATION)
        sessionToken?.let { uri.appendQueryParameter("sessionToken", it) }

        return uri.build().toString()
    }

    /**
     * [url] fit for a log line: the api key and the session token are masked, and everything
     * that tells a wrong host, customer or game apart is kept. Logcat is readable by anyone with
     * the device on a cable, and a release build can have logging switched on.
     */
    fun redacted(url: String): String {
        val uri = Uri.parse(url)
        val masked = uri.buildUpon().clearQuery()
        uri.queryParameterNames.forEach { name ->
            masked.appendQueryParameter(
                name,
                if (name in CREDENTIALS) MASK else uri.getQueryParameter(name)
            )
        }
        return masked.build().toString()
    }

    private val CREDENTIALS = setOf("apiKey", "sessionToken")
    private const val MASK = "***"

    @VisibleForTesting
    internal const val HIDE_NAVIGATION = "true"
}
