package com.gameball.gameball.model.request

import com.gameball.gameball.model.helpers.RequestModelHelpers.MapFromCustomerAttributes
import com.gameball.gameball.model.helpers.RequestModelHelpers.MapToCustomerAtrributes
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class InitializeCustomerRequest private constructor(
    @SerializedName("customerId")
    @Expose
    val customerId: String,
    
    @SerializedName("deviceToken")
    @Expose
    val deviceToken: String? = null,
    
    @SerializedName("pushServiceProvider")
    @Expose
    val pushProvider: String? = null,
    
    @SerializedName("osType")
    @Expose
    val osType: String = "Android",
    
    @SerializedName("customerAttributes")
    @Expose
    private val customerAttributesMap: MutableMap<String, Any>? = null,
    
    @SerializedName("referrerCode")
    @Expose
    val referralCode: String? = null,
    
    @SerializedName("email")
    @Expose
    val email: String? = null,
    
    @SerializedName("mobile")
    @Expose
    val mobile: String? = null,
    
    @SerializedName("guest")
    @Expose
    val isGuest: Boolean? = false
) {
    val customerAttributes: CustomerAttributes?
        get() = customerAttributesMap?.let { MapToCustomerAtrributes(it) }
    
    class Builder {
        private var customerId: String = ""
        private var deviceToken: String? = null
        private var pushProvider: String? = null
        private var customerAttributesMap: MutableMap<String, Any>? = null
        private var referralCode: String? = null
        private var email: String? = null
        private var mobile: String? = null
        private var isGuest: Boolean? = false

        fun customerId(customerId: String) = apply { this.customerId = customerId }
        fun deviceToken(deviceToken: String?) = apply { this.deviceToken = deviceToken }
        fun pushProvider(pushProvider: String?) = apply { this.pushProvider = pushProvider }
        fun referralCode(referralCode: String?) = apply { this.referralCode = referralCode }
        fun email(email: String?) = apply { this.email = email }
        fun mobile(mobile: String?) = apply { this.mobile = mobile }
        fun isGuest(isGuest: Boolean?) = apply { this.isGuest = isGuest }
        
        fun customerAttributes(customerAttributes: CustomerAttributes?) = apply {
            this.customerAttributesMap = customerAttributes?.let { MapFromCustomerAttributes(it) }
        }
        
        fun build(): InitializeCustomerRequest {
            require(customerId.isNotEmpty()) { "Customer ID cannot be empty" }
            return InitializeCustomerRequest(
                customerId = customerId,
                deviceToken = deviceToken,
                pushProvider = pushProvider,
                customerAttributesMap = customerAttributesMap ?: MapFromCustomerAttributes(CustomerAttributes.builder().build()),
                referralCode = referralCode,
                email = email,
                mobile = mobile,
                isGuest = isGuest
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
