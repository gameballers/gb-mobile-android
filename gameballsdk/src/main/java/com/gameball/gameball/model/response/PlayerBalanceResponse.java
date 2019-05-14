package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerBalanceResponse
{
    @SerializedName("PointsBalance")
    @Expose
    private String pointsBalance;
    @SerializedName("PointsValue")
    @Expose
    private double pointsValue;
    @SerializedName("Currency")
    @Expose
    private String currency;

    public String getPointsBalance()
    {
        return pointsBalance;
    }

    public double getPointsValue()
    {
        return pointsValue;
    }

    public String getCurrency()
    {
        return currency;
    }

    @Override
    public String toString()
    {
        return "PlayerBalanceResponse{" +
                "pointsBalance='" + pointsBalance + '\'' +
                ", pointsValue=" + pointsValue +
                ", currency='" + currency + '\'' +
                '}';
    }
}
