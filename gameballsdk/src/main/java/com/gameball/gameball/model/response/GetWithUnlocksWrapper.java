package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetWithUnlocksWrapper
{
    @SerializedName("response")
    @Expose
    private ArrayList<Game> games;
    @SerializedName("Success")
    @Expose
    private boolean success;
    @SerializedName("ErrorMsg")
    @Expose
    private String errorMsg;
    @SerializedName("ErrorCode")
    @Expose
    private Integer errorCode;

    public ArrayList<Game> getGames()
    {
        return games;
    }

    public void setGames(ArrayList<Game> games)
    {
        this.games = games;
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
