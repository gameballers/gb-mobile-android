package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerRegisterResponse {

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
