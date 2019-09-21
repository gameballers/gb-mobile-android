package com.gameball.gameball.views.leaderBoard;

import android.content.Context;
import android.util.Log;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.network.profileRemote.ProfileRemoteProfileDataSource;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

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
        profileRemoteDataSource.getLeaderBoard(sharedPreferencesUtils.getPlayerUniqueId())
                .subscribe(new SingleObserver<BaseResponse<ArrayList<PlayerAttributes>>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(BaseResponse<ArrayList<PlayerAttributes>> arrayListBaseResponse)
                    {
                        view.fillLeaderBoard(arrayListBaseResponse.getResponse());
                        view.hideLoadingIndicator();
                        getPlayerRank(arrayListBaseResponse.getResponse());
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideLoadingIndicator();
                        view.showNoInternetLayout();
                    }
                });
    }

    public void getPlayerRank(final ArrayList<PlayerAttributes> leaderboard)
    {
        Observable.fromIterable(leaderboard)
                .filter(new PlayerRankFilter(SharedPreferencesUtils.getInstance().getPlayerUniqueId()))
                .map(new Function<PlayerAttributes, Integer>()
                {
                    @Override
                    public Integer apply(PlayerAttributes playerAttributes) throws Exception
                    {
                        return leaderboard.indexOf(playerAttributes);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onNext(Integer integer)
                    {
                        view.onPlayerRankReady(integer, leaderboard.size());
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        Log.e("player_rank",e.getMessage());
                        view.showNoInternetLayout();
                    }

                    @Override
                    public void onComplete()
                    {
                        Log.i("player_rank","success");
                    }
                });
    }

    @Override
    public void unsubscribe()
    {
        disposable.clear();
    }
}
