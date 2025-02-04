package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CustomerRegisterResponse {

    @SerializedName("gameballId")
    @Expose()
    private String GameballId;

    public String getGameballId() {
        return GameballId;
    }
    public void setGameballId(String gameballId) {
        GameballId = gameballId;
    }
}
