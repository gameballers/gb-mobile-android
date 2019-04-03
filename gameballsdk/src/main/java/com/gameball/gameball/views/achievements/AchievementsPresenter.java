package com.gameball.gameball.views.achievements;

import android.content.Context;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class AchievementsPresenter implements AchievemetsContract.Presenter
{
    Context context;
    AchievemetsContract.View view;
    LocalDataSource localDataSource;
    ProfileRemoteDataSource remoteDataSource;
    SharedPreferencesUtils sharedPreferencesUtils;

    public AchievementsPresenter(Context context, AchievemetsContract.View view)
    {
        this.context = context;
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        remoteDataSource = ProfileRemoteDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
    }

    @Override
    public void getAchievements()
    {
        view.showLoadingIndicator();
        remoteDataSource.getWithUnlocks(sharedPreferencesUtils.getPlayerId())
                .subscribe(new SingleObserver<BaseResponse<GetWithUnlocksWrapper>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(BaseResponse<GetWithUnlocksWrapper> arrayListBaseResponse)
                    {
                        view.hideLoadingIndicator();
                        view.fillAchievements(arrayListBaseResponse.getResponse().getGames());
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideLoadingIndicator();
                    }
                });
    }
}
