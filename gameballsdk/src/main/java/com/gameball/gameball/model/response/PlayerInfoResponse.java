package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfoResponse
{
    @SerializedName("PlayerInfo")
    @Expose
    private PlayerInfo playerInfo;
    @SerializedName("NextLevel")
    @Expose
    private Level nextLevel;

    public PlayerInfo getPlayerInfo()
    {
        return playerInfo;
    }

    public Level getNextLevel()
    {
        return nextLevel;
    }
}
