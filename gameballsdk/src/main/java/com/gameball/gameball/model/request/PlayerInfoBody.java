package com.gameball.gameball.model.request;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.PlayerInfo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfoBody
{
    @SerializedName("PlayerUniqueID")
    @Expose
    private String playerUniqueId;
    @SerializedName("PlayerInfo")
    @Expose
    private PlayerInfo playerInfo;

    public PlayerInfoBody(PlayerInfo playerInfo)
    {
        playerUniqueId = SharedPreferencesUtils.getInstance().getPlayerId();
        this.playerInfo = playerInfo;
    }
}
