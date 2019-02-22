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

    public Integer getClientId()
    {
        return clientId;
    }

    public void setClientId(Integer clientId)
    {
        this.clientId = clientId;
    }

    public String getBotMainColor()
    {
        return botMainColor;
    }

    public void setBotMainColor(String botMainColor)
    {
        this.botMainColor = botMainColor;
    }

    public String getButtonBackgroundColor()
    {
        return buttonBackgroundColor;
    }

    public void setButtonBackgroundColor(String buttonBackgroundColor)
    {
        this.buttonBackgroundColor = buttonBackgroundColor;
    }

    public String getButtonFlagColor()
    {
        return buttonFlagColor;
    }

    public void setButtonFlagColor(String buttonFlagColor)
    {
        this.buttonFlagColor = buttonFlagColor;
    }

    public String getButtonSariColor()
    {
        return buttonSariColor;
    }

    public void setButtonSariColor(String buttonSariColor)
    {
        this.buttonSariColor = buttonSariColor;
    }

    public String getShape()
    {
        return shape;
    }

    public void setShape(String shape)
    {
        this.shape = shape;
    }

    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public String getOfflineStatemessage()
    {
        return offlineStatemessage;
    }

    public void setOfflineStatemessage(String offlineStatemessage)
    {
        this.offlineStatemessage = offlineStatemessage;
    }

    public String getButton()
    {
        return button;
    }

    public void setButton(String button)
    {
        this.button = button;
    }

    public String getButtonLink()
    {
        return buttonLink;
    }

    public void setButtonLink(String buttonLink)
    {
        this.buttonLink = buttonLink;
    }
}
