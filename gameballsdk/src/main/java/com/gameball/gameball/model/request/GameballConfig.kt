package com.gameball.gameball.model.request

/**
 * Configuration class for initializing the Gameball SDK
 */
data class GameballConfig private constructor(
    val apiKey: String,
    val lang: String,
    val platform: String? = null,
    val shop: String? = null,
    val apiPrefix: String? = null
) {
    class Builder {
        private var apiKey: String? = null
        private var lang: String? = null
        private var platform: String? = null
        private var shop: String? = null
        private var apiPrefix: String? = null

        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun lang(lang: String?) = apply { this.lang = lang }
        fun platform(platform: String?) = apply { this.platform = platform }
        fun shop(shop: String?) = apply { this.shop = shop }
        fun apiPrefix(apiPrefix: String?) = apply { this.apiPrefix = apiPrefix }

        fun build(): GameballConfig {
            requireNotNull(apiKey) { "API key is required" }
            requireNotNull(lang) { "Language is required" }

            return GameballConfig(
                apiKey = apiKey!!,
                lang = lang!!,
                platform = platform,
                shop = shop,
                apiPrefix = apiPrefix
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}