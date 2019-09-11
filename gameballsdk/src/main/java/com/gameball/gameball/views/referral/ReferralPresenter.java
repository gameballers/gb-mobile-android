package com.gameball.gameball.views.referral;

import android.util.Log;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.Game;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class ReferralPresenter implements ReferralContract.Presenter
{
    ReferralContract.View view;
    LocalDataSource localDataSource;
    SharedPreferencesUtils sharedPrefs;

    public ReferralPresenter(ReferralContract.View view)
    {
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        sharedPrefs = SharedPreferencesUtils.getInstance();
    }

    @Override
    public void getReferralChallenges()
    {
        Observable.fromIterable(localDataSource.games)
                .filter(new ReferralChallengesFilter())
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<Game>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(List<Game> games)
                    {
                        view.onReferralChallengesFiltered((ArrayList<Game>) games);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        Log.e("error",e.getMessage());
                    }
                });
    }
}
