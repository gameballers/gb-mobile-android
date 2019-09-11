package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReferralBody
{
    @SerializedName("PlayerCode")
    @Expose
    private String playerCode;
    @SerializedName("NewPlayerCategoryID")
    @Expose
    private Integer newPlayerCategoryId;
    @SerializedName("NewPlayerUniqueID")
    @Expose
    private String newPlayerUniqueId;

    public String getPlayerCode()
    {
        return playerCode;
    }

    public void setPlayerCode(String playerCode)
    {
        this.playerCode = playerCode;
    }

    public Integer getNewPlayerCategoryId()
    {
        return newPlayerCategoryId;
    }

    public void setNewPlayerCategoryId(Integer newPlayerCategoryId)
    {
        this.newPlayerCategoryId = newPlayerCategoryId;
    }

    public String getNewPlayerUniqueId()
    {
        return newPlayerUniqueId;
    }

    public void setNewPlayerUniqueId(String newPlayerUniqueId)
    {
        this.newPlayerUniqueId = newPlayerUniqueId;
    }
}
