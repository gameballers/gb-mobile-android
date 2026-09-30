package com.gameball.gameball.model.request

/**
 * Configuration class for initializing the Gameball SDK
 */
data class GameballConfig private constructor(
    val apiKey: String,
    val lang: String,
    val platform: String? = null,
    val shop: String? = null,
    val sessionToken: String? = null,
    val apiPrefix: String? = null,
    /**
     * The widget host, without a scheme — `m.gameball.app` by default.
     *
     * On [ShowProfileRequest] this is per call, which leaves the messaging path with no caller to
     * take it from: a host aimed at a non-production environment would get production for a
     * campaign a message opens, and that renders as "this campaign is not available" —
     * indistinguishable from a campaign that does not exist. Set it here and both paths agree.
     * A per-call value on a showProfile request still overrides this for that one presentation.
     */
    val widgetUrlPrefix: String? = null
) {
    class Builder {
        private var apiKey: String? = null
        private var lang: String? = null
        private var platform: String? = null
        private var shop: String? = null
        private var sessionToken: String? = null
        private var apiPrefix: String? = null
        private var widgetUrlPrefix: String? = null

        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun lang(lang: String?) = apply { this.lang = lang }
        fun platform(platform: String?) = apply { this.platform = platform }
        fun shop(shop: String?) = apply { this.shop = shop }
        fun sessionToken(sessionToken: String?) = apply { this.sessionToken = sessionToken }
        fun apiPrefix(apiPrefix: String?) = apply { this.apiPrefix = apiPrefix }
        fun widgetUrlPrefix(widgetUrlPrefix: String?) = apply { this.widgetUrlPrefix = widgetUrlPrefix }

        fun build(): GameballConfig {
            requireNotNull(apiKey) { "API key is required" }
            requireNotNull(lang) { "Language is required" }

            return GameballConfig(
                apiKey = apiKey!!,
                lang = lang!!,
                platform = platform,
                shop = shop,
                sessionToken = sessionToken,
                apiPrefix = apiPrefix,
                widgetUrlPrefix = widgetUrlPrefix
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}