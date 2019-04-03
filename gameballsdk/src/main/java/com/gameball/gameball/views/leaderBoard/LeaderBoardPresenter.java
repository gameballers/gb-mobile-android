package com.gameball.gameball.views.leaderBoard;

import android.content.Context;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class LeaderBoardPresenter implements LeaderBoardContract.Presenter
{
    Context context;
    LeaderBoardContract.View view;
    LocalDataSource localDataSource;
    ProfileRemoteDataSource profileRemoteDataSource;
    SharedPreferencesUtils sharedPreferencesUtils;

    public LeaderBoardPresenter(Context context, LeaderBoardContract.View view)
    {
        this.context = context;
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        profileRemoteDataSource = ProfileRemoteDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
    }

    @Override
    public void getLeaderBoard()
    {
        view.showLoadingIndicator();
        profileRemoteDataSource.getLeaderBoard(sharedPreferencesUtils.getPlayerId())
                .subscribe(new SingleObserver<BaseResponse<ArrayList<PlayerInfo>>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(BaseResponse<ArrayList<PlayerInfo>> arrayListBaseResponse)
                    {
                        view.fillLeaderBoard(arrayListBaseResponse.getResponse());
                        view.hideLoadingIndicator();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideLoadingIndicator();
                    }
                });
    }
}
