package com.gameball.gameball.model.request;

import android.support.annotation.NonNull;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;

public class Action
{
    @SerializedName("events")
    @Expose
    private HashMap<String, HashMap<String,Object>> events;
    @SerializedName("playerUniqueId")
    @Expose
    private String playerId;
    @SerializedName("isPositive")
    @Expose
    private boolean isPositive;

    public Action()
    {
        this.events = new HashMap<>();
        this.playerId = SharedPreferencesUtils.getInstance().getPlayerId();
        this.isPositive = true;
    }

    public void addEvent(String eventName, HashMap<String, Object> metaData)
    {
        if(!this.events.containsKey(eventName))
        {
            this.events.put(eventName, new HashMap<String, Object>());
        }

        this.events.put(eventName, metaData);
    }
}
