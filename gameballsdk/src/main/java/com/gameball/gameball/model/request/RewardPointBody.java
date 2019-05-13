package com.gameball.gameball.model.request;

public class RewardPointBody extends PointTransactionParams
{
    public RewardPointBody(int amount, String transactionOnClientSystemId, String transactionKey)
    {
        super(amount,transactionKey);
        setAmount(amount);
        setTransactionOnClientSystemId(transactionOnClientSystemId);

    }
}
