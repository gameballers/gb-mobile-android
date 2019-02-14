package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetNextLevelWrapper
{
    @SerializedName("response")
    @Expose
    private Level level;
    @SerializedName("Success")
    @Expose
    private boolean success;
    @SerializedName("ErrorMsg")
    @Expose
    private String errorMsg;
    @SerializedName("ErrorCode")
    @Expose
    private Integer errorCode;

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Integer getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode)
    {
        this.errorCode = errorCode;
    }
}
