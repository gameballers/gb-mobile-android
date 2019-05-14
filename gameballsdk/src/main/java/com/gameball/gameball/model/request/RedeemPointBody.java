package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RedeemPointBody extends PointTransactionParams
{
    @SerializedName("HoldReference")
    @Expose
    private String holdReference;

    public RedeemPointBody(double amount, String holdReference,
                           String transactionOnClientSystemId, String transactionKey)
    {
        super(amount,transactionKey,true);
        this.holdReference = holdReference;
        setAmount(amount);
        setTransactionOnClientSystemId(transactionOnClientSystemId);
    }
}
