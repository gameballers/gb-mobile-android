package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HoldPointBody extends PointTransactionParams
{
    @SerializedName("OTP")
    @Expose
    private String otp;

    public HoldPointBody(int amount, String otp, String transactionKey)
    {
        super(amount,transactionKey);
        this.otp = otp;
        setAmount(amount);
    }
}
