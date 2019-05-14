package com.gameball.gameball.views.leaderBoard;

import android.content.Context;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.network.profileRemote.ProfileRemoteProfileDataSource;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class LeaderBoardPresenter implements LeaderBoardContract.Presenter
{
    private Context context;
    private LeaderBoardContract.View view;
    private LocalDataSource localDataSource;
    private ProfileRemoteProfileDataSource profileRemoteDataSource;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private CompositeDisposable disposable;

    public LeaderBoardPresenter(Context context, LeaderBoardContract.View view)
    {
        this.context = context;
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        profileRemoteDataSource = ProfileRemoteProfileDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
        disposable = new CompositeDisposable();
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
                        disposable.add(d);
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

    @Override
    public void unsubscribe()
    {
        disposable.clear();
    }
}
