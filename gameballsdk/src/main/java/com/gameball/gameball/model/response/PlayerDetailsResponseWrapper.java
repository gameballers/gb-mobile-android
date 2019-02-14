package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerDetailsResponseWrapper
{
    @SerializedName("response")
    @Expose
    private PlayerDetailsResponse response;
    @SerializedName("Success")
    @Expose
    private Boolean success;
    @SerializedName("ErrorMsg")
    @Expose
    private Object errorMsg;
    @SerializedName("ErrorCode")
    @Expose
    private Integer errorCode;

    public PlayerDetailsResponse getResponse() {
        return response;
    }

    public void setResponse(PlayerDetailsResponse response) {
        this.response = response;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Object getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(Object errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

}
