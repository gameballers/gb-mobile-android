package com.gameball.gameball.model.request

import com.gameball.gameball.network.Callback

/** Builder model for showing profile */
data class ShowProfileRequest private constructor(
    val customerId: String? = null,
    val openDetail: String? = null,
    val hideNavigation: Boolean? = null,
    val showCloseButton: Boolean? = null,
    val closeButtonColor: String? = null,
    val widgetUrlPrefix: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    @Transient val externalLinkCallback: Callback<String>? = null,
    @Transient val widgetEventCallback: Callback<Map<String, Any?>>? = null
) {
    class Builder {
        private var customerId: String? = null
        private var openDetail: String? = null
        private var hideNavigation: Boolean? = null
        private var showCloseButton: Boolean? = null
        private var closeButtonColor: String? = null
        private var widgetUrlPrefix: String? = null
        private var mobile: String? = null
        private var email: String? = null
        private var externalLinkCallback: Callback<String>? = null
        private var widgetEventCallback: Callback<Map<String, Any?>>? = null

        fun customerId(customerId: String?) = apply { this.customerId = customerId }
        fun openDetail(openDetail: String?) = apply { this.openDetail = openDetail }
        fun hideNavigation(hideNavigation: Boolean?) = apply { this.hideNavigation = hideNavigation }
        fun showCloseButton(showCloseButton: Boolean?) = apply { this.showCloseButton = showCloseButton }
        fun closeButtonColor(closeButtonColor: String?) = apply { this.closeButtonColor = closeButtonColor }
        fun widgetUrlPrefix(widgetUrlPrefix: String?) = apply { this.widgetUrlPrefix = widgetUrlPrefix }
        fun mobile(mobile: String?) = apply { this.mobile = mobile }
        fun email(email: String?) = apply { this.email = email }
        fun externalLinkCallback(callback: Callback<String>?) = apply { this.externalLinkCallback = callback }
        /** Register a listener that receives widget events as a map of key-value pairs ({type, metadata}). */
        fun widgetEventCallback(callback: Callback<Map<String, Any?>>?) = apply { this.widgetEventCallback = callback }

        fun build(): ShowProfileRequest {
            return ShowProfileRequest(
                customerId = customerId,
                openDetail = openDetail,
                hideNavigation = hideNavigation,
                showCloseButton = showCloseButton,
                closeButtonColor = closeButtonColor,
                widgetUrlPrefix = widgetUrlPrefix,
                mobile = mobile,
                email = email,
                externalLinkCallback = externalLinkCallback,
                widgetEventCallback = widgetEventCallback
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}