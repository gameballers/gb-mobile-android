package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class Event
{
    @SerializedName("events")
    @Expose
    private HashMap<String, HashMap<String,Object>> events;
    @SerializedName("customerId")
    @Expose
    private String customerId;
    @SerializedName("mobile")
    @Expose
    private String mobile;

    @SerializedName("email")
    @Expose
    private String email;

    public HashMap<String, HashMap<String, Object>> getEvents() {
        return events;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public static class Builder{
        private HashMap<String, HashMap<String,Object>> events;
        private String customerId;
        private String mobile;
        private String email;
        private String eventName;

        public Builder(){

        }

        public Builder withCustomerId(String customerId){
            this.customerId = customerId;
            return this;
        }
        public Builder withEventName(String eventName){
            this.eventName = eventName;
            if(this.events == null) {
                this.events = new HashMap<String, HashMap<String, Object>>();
            }
            HashMap<String, Object> requestedEvent = this.events.get(this.eventName);
            if(requestedEvent == null){
                this.events.put(this.eventName, new HashMap<String, Object>());
            }
            return this;
        }
        public Builder withEventMetaData(String metaDataKey, Object metaDataValue){
            HashMap<String, Object> tempEvent = this.events.get(this.eventName);
            tempEvent.put(metaDataKey, metaDataValue);
            return this;
        }
        public Builder withEmail(String email){
            this.email = email;
            return this;
        }
        public Builder withMobile(String mobile){
            this.mobile = mobile;
            return this;
        }
        public Event build(){
            Event event = new Event();

            event.customerId = this.customerId;
            event.mobile = this.mobile;
            event.email = this.email;
            event.events = this.events;

            return event;
        }
    }

}