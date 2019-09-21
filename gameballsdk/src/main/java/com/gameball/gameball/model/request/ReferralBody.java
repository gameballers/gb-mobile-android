package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReferralBody
{
    @SerializedName("PlayerCode")
    @Expose
    private String playerCode;
    @SerializedName("NewPlayerTypeID")
    @Expose
    private Integer newPlayerTypeIDd;
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

    public Integer getNewPlayerTypeIDd()
    {
        return newPlayerTypeIDd;
    }

    public void setNewPlayerTypeIDd(Integer newPlayerTypeIDd)
    {
        this.newPlayerTypeIDd = newPlayerTypeIDd;
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
