package com.gameball.gameball.views.profile;

import android.content.Context;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

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
                            view.hideLoadingIndicator();
                        }
                    });
        }
    }

    @Override
    public void getWithUnlocks()
    {
        view.showLoadingIndicator();
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
                        localDataSource.quests = arrayListBaseResponse.getResponse().getQuests();

                        view.onWithUnlocksLoaded(localDataSource.games, localDataSource.quests);
                        view.hideLoadingIndicator();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        e.printStackTrace();
                        view.hideLoadingIndicator();
                    }
                });
    }
}
