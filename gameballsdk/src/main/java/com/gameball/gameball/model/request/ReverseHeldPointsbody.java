package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReverseHeldPointsbody extends PointTransactionParams
{
    @SerializedName("HoldReference")
    @Expose
    private String holdReference;


    public ReverseHeldPointsbody(String holdReference, String transactionKey)
    {
        super(transactionKey, true);
        this.holdReference = holdReference;

    }
}
