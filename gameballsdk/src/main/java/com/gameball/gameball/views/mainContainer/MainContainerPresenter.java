package com.gameball.gameball.views.mainContainer;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.network.profileRemote.ProfileRemoteProfileDataSource;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MainContainerPresenter implements MainContainerContract.Presenter
{
    private MainContainerContract.View view;
    private LocalDataSource localDataSource;
    private ProfileRemoteProfileDataSource profileRemoteDataSource;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private CompositeDisposable disposable;

    public MainContainerPresenter(MainContainerContract.View view)
    {
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        profileRemoteDataSource = ProfileRemoteProfileDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
        disposable = new CompositeDisposable();
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
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(BaseResponse<PlayerInfoResponse> playerInfoResponseBaseResponse)
                    {
                        localDataSource.playerInfo = playerInfoResponseBaseResponse.getResponse().
                                getPlayerInfo();
                        localDataSource.nextLevel = playerInfoResponseBaseResponse.getResponse().
                                getNextLevel();
                        view.hideLoadingIndicator();
                        view.onProfileInfoLoaded(localDataSource.playerInfo, localDataSource.nextLevel);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        e.printStackTrace();
                        view.hideLoadingIndicator();
                    }
                });
    }

    @Override
    public void unsubscribe()
    {
        disposable.clear();
    }
}
