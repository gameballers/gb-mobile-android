package com.gameball.gameball.network.transactionRemote;

import com.gameball.gameball.model.request.RewardPointsBody;
import com.gameball.gameball.model.response.BaseResponse;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionDataSourceContract
{
    Completable RewardPoints(RewardPointsBody body);
}
