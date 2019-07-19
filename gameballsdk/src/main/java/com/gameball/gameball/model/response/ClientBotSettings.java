package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientBotSettings
{
    @SerializedName("ClientId")
    @Expose
    private Integer clientId;
    @SerializedName("BotMainColor")
    @Expose
    private String botMainColor;
    @SerializedName("ButtonBackgroundColor")
    @Expose
    private String buttonBackgroundColor;
    @SerializedName("ButtonFlagColor")
    @Expose
    private String buttonFlagColor;
    @SerializedName("ButtonSariColor")
    @Expose
    private String buttonSariColor;
    @SerializedName("Shape")
    @Expose
    private String shape;
    @SerializedName("Direction")
    @Expose
    private String direction;
    @SerializedName("OfflineStatemessage")
    @Expose
    private String offlineStatemessage;
    @SerializedName("Button")
    @Expose
    private String button;
    @SerializedName("ButtonLink")
    @Expose
    private String buttonLink;
    @SerializedName("EnableLeaderboard")
    @Expose
    private boolean enableLeaderboard;
    @SerializedName("ClientFirebase")
    @Expose
    private ClientFireBase clientFireBase;

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
