package com.gameball.gameball.model.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Event private constructor(
    @SerializedName("events")
    @Expose
    val events: MutableMap<String, MutableMap<String, Any>>,
    
    @SerializedName("customerId")
    @Expose
    val customerId: String,
    
    @SerializedName("mobile")
    @Expose
    val mobile: String? = null,
    
    @SerializedName("email")
    @Expose
    val email: String? = null
) {
    class Builder {
        private var events: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
        private var customerId: String = ""
        private var mobile: String? = null
        private var email: String? = null
        private var currentEventName: String? = null

        fun customerId(customerId: String) = apply { this.customerId = customerId }
        
        fun eventName(eventName: String?) = apply {
            this.currentEventName = eventName
            if (eventName != null && events[eventName] == null) {
                events[eventName] = mutableMapOf()
            }
        }
        
        fun eventMetaData(key: String, value: Any) = apply {
            require(currentEventName != null) { "Event name must be set before adding metadata" }
            events[currentEventName]?.set(key, value)
        }
        
        fun email(email: String?) = apply { this.email = email }
        
        fun mobile(mobile: String?) = apply { this.mobile = mobile }
        
        fun build(): Event {
            require(customerId.isNotEmpty()) { "Customer ID cannot be empty" }
            require(events.isNotEmpty()) { "At least one event must be added" }
            return Event(
                events = events,
                customerId = customerId,
                mobile = mobile,
                email = email
            )
        }
    }
    
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}