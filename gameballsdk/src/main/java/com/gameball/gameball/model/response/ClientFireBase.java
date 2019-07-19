package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientFireBase
{
    @SerializedName("id")
    @Expose
    private int id;
    @SerializedName("ApplicationId")
    @Expose
    private String applicationId;
    @SerializedName("SenderId")
    @Expose
    private String senderId;
    @SerializedName("WebAPIKey")
    @Expose
    private String webApiKey;
    @SerializedName("ClientId")
    @Expose
    private int clientId;

    public int getId()
    {
        return id;
    }

    public String getApplicationId()
    {
        return applicationId;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public int getClientId()
    {
        return clientId;
    }

    public String getWebApiKey()
    {
        return webApiKey;
    }
}
