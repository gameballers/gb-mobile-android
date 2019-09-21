package com.gameball.gameball.model.request;

import com.gameball.gameball.model.response.PlayerAttributes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class PlayerRegisterRequest {
    @SerializedName("playerTypeId")
    @Expose
    private Integer playerTypeID;
    @SerializedName("playerUniqueId")
    @Expose
    private String playerUniqueID;
    @SerializedName("deviceToken")
    @Expose
    private String deviceToken;
    @SerializedName("osType")
    @Expose
    private String oSType = "Android";
    @SerializedName("playerAttributes")
    @Expose
    private PlayerAttributes playerAttributes;

    public Integer getPlayerTypeID()
    {
        return playerTypeID;
    }

    public void setPlayerTypeID(Integer playerTypeID)
    {
        this.playerTypeID = playerTypeID;
    }

    public String getPlayerUniqueID()
    {
        return playerUniqueID;
    }

    public void setPlayerUniqueID(String playerUniqueID)
    {
        this.playerUniqueID = playerUniqueID;
    }

    public String getDeviceToken()
    {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken)
    {
        this.deviceToken = deviceToken;
    }

    public PlayerAttributes getPLayerInfo()
    {
        return playerAttributes;
    }

    public void setPlayerAttributes(PlayerAttributes playerAttributes)
    {
        this.playerAttributes = playerAttributes;
    }
}
