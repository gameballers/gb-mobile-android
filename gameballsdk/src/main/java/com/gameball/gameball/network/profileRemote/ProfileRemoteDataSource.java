package com.gameball.gameball.network.profileRemote;

import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;
import com.gameball.gameball.network.ServiceBuilder;
import com.gameball.gameball.network.api.GameBallApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProfileRemoteDataSource implements DataSourceContract
{
    private static ProfileRemoteDataSource instance;
    private GameBallApi gameBallApi;
    private Gson jsonFactory;

    private ProfileRemoteDataSource()
    {
        jsonFactory = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        gameBallApi = ServiceBuilder.buildService(GameBallApi.class, jsonFactory);
    }

    public static ProfileRemoteDataSource getInstance()
    {
        if(instance == null)
            instance = new ProfileRemoteDataSource();

        return instance;
    }

    @Override
    public Single<BaseResponse<PlayerDetailsResponse>> getPlayerDetails(String playerId)
    {
        return gameBallApi.getPlayerDetails(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<ArrayList<Game>>> getWithUnlocks(String playerId)
    {
        return gameBallApi.getWithUnlocks(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<Level>> getNextLevel(String playerId)
    {
        return gameBallApi.getNextLevel(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<ArrayList<PlayerDetailsResponse>>> getLeaderBoard(String playerId)
    {
        return gameBallApi.getLeaderBoard(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<ClientBotSettings>> getBotSettings()
    {
        return gameBallApi.getBotSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
