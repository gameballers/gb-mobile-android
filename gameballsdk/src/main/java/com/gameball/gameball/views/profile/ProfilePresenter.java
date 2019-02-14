package com.gameball.gameball.views.profile;

import android.content.Context;
import android.util.Pair;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.model.response.GetNextLevelWrapper;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.PlayerDetailsResponseWrapper;
import com.gameball.gameball.network.profileRemote.ProfileRemoteDataSource;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

public class ProfilePresenter implements ProfileContract.Presenter
{
    Context context;
    ProfileContract.View view;
    LocalDataSource localDataSource;
    ProfileRemoteDataSource profileRemoteDataSource;

    public ProfilePresenter(Context context, ProfileContract.View view)
    {
        this.context = context;
        this.view = view;
        profileRemoteDataSource = ProfileRemoteDataSource.getInstance();
        localDataSource = LocalDataSource.getInstance();
    }

    @Override
    public void getPlayerDetails()
    {
        view.showProfileLoadingIndicator();

        Observable<PlayerDetailsResponseWrapper> playerDetailsObservable =
                profileRemoteDataSource.getPlayerDetails(localDataSource.playerId)
                .observeOn(AndroidSchedulers.mainThread());

        Observable<GetNextLevelWrapper> nextLevelObservable =
                profileRemoteDataSource.getNextLevel(localDataSource.playerId)
                        .observeOn(AndroidSchedulers.mainThread());

        Observable.zip(playerDetailsObservable, nextLevelObservable
                , new BiFunction<PlayerDetailsResponseWrapper, GetNextLevelWrapper,
                        Pair<PlayerDetailsResponseWrapper, GetNextLevelWrapper>>()
                {
                    @Override
                    public Pair<PlayerDetailsResponseWrapper, GetNextLevelWrapper> apply(PlayerDetailsResponseWrapper playerDetailsResponseWrapper, GetNextLevelWrapper getNextLevelWrapper) throws Exception
                    {
                        return new Pair<>(playerDetailsResponseWrapper, getNextLevelWrapper);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Pair<PlayerDetailsResponseWrapper, GetNextLevelWrapper>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onNext(
                            Pair<PlayerDetailsResponseWrapper, GetNextLevelWrapper> playerDetailsAndNextLevelPair)
                    {
                        localDataSource.playerDetails = playerDetailsAndNextLevelPair.first.getResponse();
                        localDataSource.nextLevel = playerDetailsAndNextLevelPair.second.getLevel();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideProfileLoadingIndicator();
                    }

                    @Override
                    public void onComplete()
                    {
                        view.hideProfileLoadingIndicator();
                        view.fillPlayerData(localDataSource.playerDetails, localDataSource.nextLevel);
                    }
                });
    }

    @Override
    public void getWithUnlocks()
    {
        view.showAchievementsLoadingIndicator();
        profileRemoteDataSource.getWithUnlocks(localDataSource.playerId)
                .subscribe(new Observer<GetWithUnlocksWrapper>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onNext(GetWithUnlocksWrapper getWithUnlocksWrapper)
                    {
                        localDataSource.games = getWithUnlocksWrapper.getGames();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        view.hideAchievementsLoadingIndicator();
                    }

                    @Override
                    public void onComplete()
                    {
                        view.hideAchievementsLoadingIndicator();
                        view.fillAchievements(localDataSource.games);
                    }
                });
    }
}
