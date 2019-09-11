package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientFireBase
{
    @SerializedName("applicationId")
    @Expose
    private String applicationId;
    @SerializedName("senderId")
    @Expose
    private String senderId;
    @SerializedName("webAPIKey")
    @Expose
    private String webApiKey;

    public String getApplicationId()
    {
        return applicationId;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public String getWebApiKey()
    {
        return webApiKey;
    }
}
