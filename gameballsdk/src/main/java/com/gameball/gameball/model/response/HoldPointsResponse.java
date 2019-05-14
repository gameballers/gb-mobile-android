package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HoldPointsResponse
{
    @SerializedName("HoldReference")
    @Expose
    private String holdReference;

    public String getHoldReference()
    {
        return holdReference;
    }

    public void setHoldReference(String holdReference)
    {
        this.holdReference = holdReference;
    }

    @Override
    public String toString()
    {
        return "HoldPointsResponse{" +
                "holdReference='" + holdReference + '\'' +
                '}';
    }
}
