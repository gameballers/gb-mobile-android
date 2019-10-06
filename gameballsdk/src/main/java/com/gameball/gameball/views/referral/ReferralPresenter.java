package com.gameball.gameball.views.referral;

import android.util.Log;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.network.profileRemote.ProfileRemoteProfileDataSource;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ReferralPresenter implements ReferralContract.Presenter
{
    private ReferralContract.View view;
    private LocalDataSource localDataSource;
    private SharedPreferencesUtils sharedPrefs;
    private ProfileRemoteProfileDataSource profileRemoteProfileDataSource;

    public ReferralPresenter(ReferralContract.View view)
    {
        this.view = view;
        localDataSource = LocalDataSource.getInstance();
        sharedPrefs = SharedPreferencesUtils.getInstance();
        this.profileRemoteProfileDataSource = ProfileRemoteProfileDataSource.getInstance();
    }

    @Override
    public void getReferralChallenges()
    {
        profileRemoteProfileDataSource.getWithUnlocks(sharedPrefs.getPlayerUniqueId())
                .flattenAsObservable(new Function<BaseResponse<GetWithUnlocksWrapper>, Iterable<Game>>() {
                    @Override
                    public Iterable<Game> apply(BaseResponse<GetWithUnlocksWrapper> getWithUnlocksWrapperBaseResponse) throws Exception {
                        return getWithUnlocksWrapperBaseResponse.getResponse().getGames();
                    }
                })
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
