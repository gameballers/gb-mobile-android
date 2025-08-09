package com.gameball.gameball.model.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CustomerAttributes private constructor(
    @SerializedName("displayName")
    @Expose
    val displayName: String? = null,
    
    @SerializedName("firstName")
    @Expose
    val firstName: String? = null,
    
    @SerializedName("lastName")
    @Expose
    val lastName: String? = null,
    
    @SerializedName("email")
    @Expose
    val email: String? = null,
    
    @SerializedName("gender")
    @Expose
    val gender: String? = null,
    
    @SerializedName("mobile")
    @Expose
    val mobile: String? = null,
    
    @SerializedName("dateOfBirth")
    @Expose
    val dateOfBirth: String? = null,
    
    @SerializedName("joinDate")
    @Expose
    val joinDate: String? = null,
    
    @SerializedName("preferredLanguage")
    @Expose
    val preferredLanguage: String? = null,
    
    @SerializedName("channel")
    @Expose
    val channel: String = "mobile",
    
    @SerializedName("custom")
    val customAttributes: MutableMap<String, String>? = null,
    
    val additionalAttributes: MutableMap<String, String>? = null
) {
    class Builder {
        private var displayName: String? = null
        private var firstName: String? = null
        private var lastName: String? = null
        private var email: String? = null
        private var gender: String? = null
        private var mobile: String? = null
        private var dateOfBirth: String? = null
        private var joinDate: String? = null
        private var preferredLanguage: String? = null
        private var channel: String = "mobile"
        private var customAttributes: MutableMap<String, String>? = null
        private var additionalAttributes: MutableMap<String, String>? = null

        fun displayName(displayName: String?) = apply { this.displayName = displayName }
        fun firstName(firstName: String?) = apply { this.firstName = firstName }
        fun lastName(lastName: String?) = apply { this.lastName = lastName }
        fun email(email: String?) = apply { this.email = email }
        fun gender(gender: String?) = apply { this.gender = gender }
        fun mobile(mobile: String?) = apply { this.mobile = mobile }
        fun dateOfBirth(dateOfBirth: String?) = apply { this.dateOfBirth = dateOfBirth }
        fun joinDate(joinDate: String?) = apply { this.joinDate = joinDate }
        fun preferredLanguage(preferredLanguage: String?) = apply { this.preferredLanguage = preferredLanguage }

        fun addCustomAttribute(key: String, value: String) = apply {
            if (customAttributes == null) customAttributes = mutableMapOf()
            customAttributes!![key] = value
        }
        
        fun customAttributes(attributes: Map<String, String>?) = apply {
            this.customAttributes = attributes?.toMutableMap()
        }
        
        fun addAdditionalAttribute(key: String, value: String) = apply {
            if (additionalAttributes == null) additionalAttributes = mutableMapOf()
            additionalAttributes!![key] = value
        }
        
        fun additionalAttributes(attributes: Map<String, String>?) = apply {
            this.additionalAttributes = attributes?.toMutableMap()
        }
        
        fun build(): CustomerAttributes {
            return CustomerAttributes(
                displayName = displayName,
                firstName = firstName,
                lastName = lastName,
                email = email,
                gender = gender,
                mobile = mobile,
                dateOfBirth = dateOfBirth,
                joinDate = joinDate,
                preferredLanguage = preferredLanguage,
                channel = channel,
                customAttributes = customAttributes,
                additionalAttributes = additionalAttributes
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}