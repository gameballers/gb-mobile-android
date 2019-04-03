package com.gameball.gameball.views.profile;

import android.content.Context;
import android.util.Pair;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

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
    public void getPlayerInfo(boolean fromLocal)
    {
        if(fromLocal)
        {
            view.fillPlayerData(localDataSource.playerInfo,localDataSource.nextLevel);
        }
        else
        {
            view.showLoadingIndicator();
            profileRemoteDataSource.getPlayerInfo(sharedPreferencesUtils.getPlayerId())
                    .subscribe(new SingleObserver<BaseResponse<PlayerInfoResponse>>()
                    {
                        @Override
                        public void onSubscribe(Disposable d)
                        {

                        }

                        @Override
                        public void onSuccess(BaseResponse<PlayerInfoResponse> playerInfoResponseBaseResponse)
                        {
                            localDataSource.playerInfo = playerInfoResponseBaseResponse.getResponse().
                                    getPlayerInfo();
                            localDataSource.nextLevel = playerInfoResponseBaseResponse.getResponse().
                                    getNextLevel();
                            view.hideLoadingIndicator();
                            view.fillPlayerData(localDataSource.playerInfo, localDataSource.nextLevel);
                        }

                        @Override
                        public void onError(Throwable e)
                        {

                        }
                    });
        }
    }

    @Override
    public void getWithUnlocks()
    {
        profileRemoteDataSource.getWithUnlocks(sharedPreferencesUtils.getPlayerId())
                .subscribe(new SingleObserver<BaseResponse<GetWithUnlocksWrapper>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(BaseResponse<GetWithUnlocksWrapper> arrayListBaseResponse)
                    {
                        localDataSource.games = arrayListBaseResponse.getResponse().getGames();
                        view.fillAchievements(localDataSource.games);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        e.printStackTrace();
                    }
                });
    }
}
