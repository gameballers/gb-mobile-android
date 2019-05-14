package com.gameball.gameball.model.request;

public class GenerateOTPBody extends PointTransactionParams
{
    public GenerateOTPBody(String transactionKey)
    {
        super(transactionKey,true);
    }
}
