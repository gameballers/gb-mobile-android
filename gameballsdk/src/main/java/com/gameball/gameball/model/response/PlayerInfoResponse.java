package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfoResponse
{
    @SerializedName("playerInfo")
    @Expose
    private PlayerInfo playerInfo;
    @SerializedName("nextLevel")
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
