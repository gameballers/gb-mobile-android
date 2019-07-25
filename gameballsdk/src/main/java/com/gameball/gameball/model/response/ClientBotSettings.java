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
    @SerializedName("buttonBackgroundColor")
    @Expose
    private String buttonBackgroundColor;
    @SerializedName("buttonFlagColor")
    @Expose
    private String buttonFlagColor;
    @SerializedName("buttonSariColor")
    @Expose
    private String buttonSariColor;
    @SerializedName("Shape")
    @Expose
    private String shape;
    @SerializedName("Direction")
    @Expose
    private String direction;
    @SerializedName("offlineStatemessage")
    @Expose
    private String offlineStatemessage;
    @SerializedName("button")
    @Expose
    private String button;
    @SerializedName("buttonLink")
    @Expose
    private String buttonLink;
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

    public String getButtonBackgroundColor()
    {
        return buttonBackgroundColor;
    }

    public String getButtonFlagColor()
    {
        return buttonFlagColor;
    }

    public String getButtonSariColor()
    {
        return buttonSariColor;
    }

    public String getShape()
    {
        return shape;
    }

    public String getDirection()
    {
        return direction;
    }

    public String getOfflineStatemessage()
    {
        return offlineStatemessage;
    }

    public String getButton()
    {
        return button;
    }

    public String getButtonLink()
    {
        return buttonLink;
    }

    public boolean isEnableLeaderboard()
    {
        return enableLeaderboard;
    }

    public ClientFireBase getClientFireBase()
    {
        return clientFireBase;
    }
}
