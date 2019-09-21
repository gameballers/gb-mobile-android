package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfoResponse
{
    @SerializedName("playerInfo")
    @Expose
    private PlayerAttributes playerAttributes;
    @SerializedName("nextLevel")
    @Expose
    private Level nextLevel;

    public PlayerAttributes getPlayerAttributes()
    {
        return playerAttributes;
    }

    public Level getNextLevel()
    {
        return nextLevel;
    }

}
