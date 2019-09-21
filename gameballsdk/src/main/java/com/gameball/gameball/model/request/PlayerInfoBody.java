package com.gameball.gameball.model.request;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfoBody
{
    @SerializedName("PlayerUniqueID")
    @Expose
    private String playerUniqueId;
    @SerializedName("playerAttributes")
    @Expose
    private PlayerAttributes playerAttributes;

    public PlayerInfoBody(PlayerAttributes playerAttributes)
    {
        playerUniqueId = SharedPreferencesUtils.getInstance().getPlayerUniqueId();
        this.playerAttributes = playerAttributes;
    }
}
