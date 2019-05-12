package com.gameball.gameball.network.transactionRemote;

import com.gameball.gameball.model.request.RewardPointsBody;
import com.gameball.gameball.network.Network;
import com.gameball.gameball.network.api.GameBallApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TransactionRemoteDataSource implements TransactionDataSourceContract
{
    private static TransactionRemoteDataSource instance;
    private GameBallApi gameBallApi;
    private Gson jsonFactory;

    private TransactionRemoteDataSource()
    {
        jsonFactory = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        gameBallApi = Network.getInstance().getGameBallApi();
    }

    public static TransactionRemoteDataSource getInstance()
    {
        if (instance == null)
            instance = new TransactionRemoteDataSource();

        return instance;
    }

    @Override
    public Completable RewardPoints(RewardPointsBody body)
    {
        return gameBallApi.rewardPoints(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
