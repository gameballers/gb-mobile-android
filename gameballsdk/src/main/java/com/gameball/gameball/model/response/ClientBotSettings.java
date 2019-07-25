package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientBotSettings
{
    @SerializedName("ClientId")
    @Expose
    private Integer clientId;
    @SerializedName("botMainColor")
    @Expose
    private String botMainColor;
    @SerializedName("Shape")
    @Expose
    private String shape;
    @SerializedName("Direction")
    @Expose
    private String direction;
    @SerializedName("enableLeaderboard")
    @Expose
    private boolean enableLeaderboard;
    @SerializedName("isReferralOn")
    @Expose
    private boolean isReferralOn;
    @SerializedName("enableNotifications")
    @Expose
    private boolean enableNotifications;
    @SerializedName("ClientFirebase")
    @Expose
    private ClientFireBase clientFireBase;
    @SerializedName("isBotDarkTheme")
    @Expose
    private boolean isBotDarkTheme;
    @SerializedName("isRankPointsVisible")
    @Expose
    private boolean isRankPointsVisible;
    @SerializedName("isWalletPointsVisible")
    @Expose
    private boolean isWalletPointsVisible;
    @SerializedName("rankPointsName")
    @Expose
    private String rankPointsName;
    @SerializedName("walletPointsName")
    @Expose
    private String walletPointsName;

    public Integer getClientId()
    {
        return clientId;
    }

    public String getBotMainColor()
    {
        return botMainColor;
    }

    public String getShape()
    {
        return shape;
    }

    public String getDirection()
    {
        return direction;
    }

    public boolean isEnableLeaderboard()
    {
        return enableLeaderboard;
    }

    public ClientFireBase getClientFireBase()
    {
        return clientFireBase;
    }

    public boolean isReferralOn()
    {
        return isReferralOn;
    }

    public boolean isEnableNotifications()
    {
        return enableNotifications;
    }

    public boolean isBotDarkTheme()
    {
        return isBotDarkTheme;
    }

    public boolean isRankPointsVisible()
    {
        return isRankPointsVisible;
    }

    public boolean isWalletPointsVisible()
    {
        return isWalletPointsVisible;
    }

    public String getRankPointsName()
    {
        return rankPointsName;
    }

    public String getWalletPointsName()
    {
        return walletPointsName;
    }
}
