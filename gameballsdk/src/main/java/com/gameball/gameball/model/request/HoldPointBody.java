package com.gameball.gameball.model.request;

public class HoldPointBody extends PointTransactionParams
{
    public HoldPointBody(int amount, String transactionKey)
    {
        super(transactionKey);
        setAmount(amount);
    }
}
