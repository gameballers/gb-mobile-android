package com.gameball.gameball.network.profileRemote;

import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.model.request.PlayerInfoBody;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.Notification;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.network.Network;
import com.gameball.gameball.network.api.GameBallApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProfileRemoteProfileDataSource implements ProfileDataSourceContract
{
    private static ProfileRemoteProfileDataSource instance;
    private GameBallApi gameBallApi;
    private Gson jsonFactory;

    private ProfileRemoteProfileDataSource()
    {
        jsonFactory = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        gameBallApi = Network.getInstance().getGameBallApi();
    }

    public static ProfileRemoteProfileDataSource getInstance()
    {
        if(instance == null)
            instance = new ProfileRemoteProfileDataSource();

        return instance;
    }

    @Override
    public Single<BaseResponse<PlayerInfoResponse>> getPlayerInfo(String playerUniqueId)
    {
        return gameBallApi.getPlayerDetails(playerUniqueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<GetWithUnlocksWrapper>> getWithUnlocks(String playerUniqueId)
    {
        return gameBallApi.getWithUnlocks(playerUniqueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<ArrayList<PlayerAttributes>>> getLeaderBoard(String playerUniqueId)
    {
        return gameBallApi.getLeaderBoard(playerUniqueId)
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

    @Override
    public Completable AddNewAction(Action actionBody)
    {
        return gameBallApi.addNewAtion(actionBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Completable initializePlayer(PlayerInfoBody body)
    {
        return gameBallApi.initializePlayer(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<BaseResponse<ArrayList<Notification>>> getNotificationHistory(String playerUniqueId) {
        return gameBallApi.getPlayerNotificationHistory(playerUniqueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
