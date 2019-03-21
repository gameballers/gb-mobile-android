package com.gameball.gameball.views.profile;

import android.content.Context;
import android.util.Pair;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

public class ProfilePresenter implements ProfileContract.Presenter
{
    Context context;
    ProfileContract.View view;
    LocalDataSource localDataSource;
    ProfileRemoteDataSource profileRemoteDataSource;
    SharedPreferencesUtils sharedPreferencesUtils;

    public ProfilePresenter(Context context, ProfileContract.View view)
    {
        this.context = context;
        this.view = view;
        profileRemoteDataSource = ProfileRemoteDataSource.getInstance();
        localDataSource = LocalDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
    }

    @Override
    public void getPlayerDetails()
    {
        view.showLoadingIndicator();

        Single<BaseResponse<PlayerDetailsResponse>> playerDetailsObservable =
                profileRemoteDataSource.getPlayerDetails(sharedPreferencesUtils.getPlayerId())
                .observeOn(AndroidSchedulers.mainThread());

        Single<BaseResponse<Level>> nextLevelObservable =
                profileRemoteDataSource.getNextLevel(sharedPreferencesUtils.getPlayerId())
                        .observeOn(AndroidSchedulers.mainThread());

        Single.zip(playerDetailsObservable, nextLevelObservable
                , new BiFunction<BaseResponse<PlayerDetailsResponse>, BaseResponse<Level>,
                        Pair<BaseResponse<PlayerDetailsResponse>, BaseResponse<Level>>>()
                {
                    @Override
                    public Pair<BaseResponse<PlayerDetailsResponse>, BaseResponse<Level>> apply(BaseResponse<PlayerDetailsResponse> playerDetailsResponseWrapper, BaseResponse<Level> getNextLevelWrapper) throws Exception
                    {
                        return new Pair<>(playerDetailsResponseWrapper, getNextLevelWrapper);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Pair<BaseResponse<PlayerDetailsResponse>, BaseResponse<Level>>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(Pair<BaseResponse<PlayerDetailsResponse>, BaseResponse<Level>> playerDetailsAndNextLevelPair)
                    {
                        localDataSource.playerDetails = playerDetailsAndNextLevelPair.first.getResponse();
                        localDataSource.nextLevel = playerDetailsAndNextLevelPair.second.getResponse();
                        view.hideLoadingIndicator();
                        view.fillPlayerData(localDataSource.playerDetails, localDataSource.nextLevel);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideLoadingIndicator();
                    }
                });
    }

    @Override
    public void getWithUnlocks()
    {
        profileRemoteDataSource.getWithUnlocks(sharedPreferencesUtils.getPlayerId())
                .subscribe(new SingleObserver<BaseResponse<ArrayList<Game>>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(BaseResponse<ArrayList<Game>> arrayListBaseResponse)
                    {
                        localDataSource.games = arrayListBaseResponse.getResponse();
                        view.fillAchievements(localDataSource.games);
                    }

                    @Override
                    public void onError(Throwable e)
                    {

                    }
                });
    }
}
