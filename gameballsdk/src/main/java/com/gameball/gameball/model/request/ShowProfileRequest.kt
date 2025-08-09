package com.gameball.gameball.model.request

import com.gameball.gameball.network.Callback

/** Builder model for showing profile */
data class ShowProfileRequest private constructor(
    val customerId: String,
    val openDetail: String? = null,
    val hideNavigation: Boolean? = null,
    val showCloseButton: Boolean? = null,
    val closeButtonColor: String? = null,
    val widgetUrlPrefix: String? = null,
    val capturedLinkCallback: Callback<String>? = null
) {
    class Builder {
        private var customerId: String = ""
        private var openDetail: String? = null
        private var hideNavigation: Boolean? = null
        private var showCloseButton: Boolean? = null
        private var closeButtonColor: String? = null
        private var widgetUrlPrefix: String? = null
        private var capturedLinkCallback: Callback<String>? = null

        fun customerId(customerId: String) = apply { this.customerId = customerId }
        fun openDetail(openDetail: String?) = apply { this.openDetail = openDetail }
        fun hideNavigation(hideNavigation: Boolean?) = apply { this.hideNavigation = hideNavigation }
        fun showCloseButton(showCloseButton: Boolean?) = apply { this.showCloseButton = showCloseButton }
        fun closeButtonColor(closeButtonColor: String?) = apply { this.closeButtonColor = closeButtonColor }
        fun widgetUrlPrefix(widgetUrlPrefix: String?) = apply { this.widgetUrlPrefix = widgetUrlPrefix }
        fun capturedLinkCallback(callback: Callback<String>?) = apply { this.capturedLinkCallback = callback }

        fun build(): ShowProfileRequest {
            require(customerId.isNotEmpty()) { "Customer ID cannot be empty" }
            return ShowProfileRequest(
                customerId = customerId,
                openDetail = openDetail,
                hideNavigation = hideNavigation,
                showCloseButton = showCloseButton,
                closeButtonColor = closeButtonColor,
                widgetUrlPrefix = widgetUrlPrefix,
                capturedLinkCallback = capturedLinkCallback
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}