package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReverseHeldPointsbody extends PointTransactionParams
{
    public ReverseHeldPointsbody(String transactionKey)
    {
        super(transactionKey, true);
    }
}
