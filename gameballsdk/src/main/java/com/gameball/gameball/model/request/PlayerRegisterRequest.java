package com.gameball.gameball.model.request;

import static com.gameball.gameball.model.helpers.RequestModelHelpers.*;

import com.gameball.gameball.model.response.PlayerAttributes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

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
    private HashMap<String, Object> playerAttributes;
    @SerializedName("referrerCode")
    @Expose
    private String ReferrerCode;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("mobile")
    @Expose
    private String mobile;

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
        return MapToPlayerAtrributes(playerAttributes);
    }

    public void setPlayerAttributes(PlayerAttributes playerAttributes)
    {
        this.playerAttributes = MapFromPlayerAttributes(playerAttributes);
    }

    public void setReferrerCode(String referrerCode){
        this.ReferrerCode = referrerCode;
    }

    public String getReferrerCode(){
        return this.ReferrerCode;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getEmail(){
        return this.email;
    }

    public void setMobile(String mobile){
        this.mobile = mobile;
    }

    public String getMobile(){
        return this.mobile;
    }
}
