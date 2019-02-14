package com.gameball.gameball.network.profileRemote;

import android.graphics.Bitmap;

import com.gameball.gameball.model.response.GetNextLevelWrapper;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.PlayerDetailsResponseWrapper;
import com.gameball.gameball.network.GenericCallback;
import com.gameball.gameball.network.ServiceBuilder;
import com.gameball.gameball.network.api.GameBallApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    public Observable<PlayerDetailsResponseWrapper> getPlayerDetails(String playerId)
    {
        return gameBallApi.getPlayerDetails(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<GetWithUnlocksWrapper> getWithUnlocks(String playerId)
    {
        return gameBallApi.getWithUnlocks(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<GetNextLevelWrapper> getNextLevel(String playerId)
    {
        return gameBallApi.getNextLevel(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
