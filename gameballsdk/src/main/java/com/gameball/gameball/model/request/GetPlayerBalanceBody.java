package com.gameball.gameball.model.request;

public class GetPlayerBalanceBody extends PointTransactionParams
{
    public GetPlayerBalanceBody(String transactionKey)
    {
        super(transactionKey, false);
    }
}
