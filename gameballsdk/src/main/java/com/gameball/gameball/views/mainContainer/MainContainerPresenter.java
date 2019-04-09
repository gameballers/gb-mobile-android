package com.gameball.gameball.views.mainContainer;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class MainContainerPresenter implements MainContainerContract.Presenter
{
    MainContainerContract.View view;
    LocalDataSource localDataSource;
    ProfileRemoteDataSource profileRemoteDataSource;
    SharedPreferencesUtils sharedPreferencesUtils;

    public MainContainerPresenter(MainContainerContract.View view)
    {
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        profileRemoteDataSource = ProfileRemoteDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
    }

    @Override
    public void getPlayerInfo()
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
                        view.onProfileInfoLoaded(localDataSource.playerInfo);
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
