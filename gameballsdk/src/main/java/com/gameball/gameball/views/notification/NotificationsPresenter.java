package com.gameball.gameball.views.notification;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Notification;
import com.gameball.gameball.network.profileRemote.ProfileRemoteProfileDataSource;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class NotificationsPresenter implements NotificationsContract.Presenter {

    NotificationsContract.View view;
    private ProfileRemoteProfileDataSource profileRemoteDataSource;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private CompositeDisposable disposable;

    public NotificationsPresenter(NotificationsContract.View view) {
        this.view = view;
        profileRemoteDataSource = ProfileRemoteProfileDataSource.getInstance();
        sharedPreferencesUtils = SharedPreferencesUtils.getInstance();
        disposable = new CompositeDisposable();

    }

    @Override
    public void getNotificationHistory() {
        view.showLoadingIndicator();

        profileRemoteDataSource.getNotificationHistory(sharedPreferencesUtils.getPlayerUniqueId())
                .subscribe(new SingleObserver<BaseResponse<ArrayList<Notification>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(BaseResponse<ArrayList<Notification>> arrayListBaseResponse) {
                        view.hideLoadingIndicator();
                        view.onNotificationsLoaded(arrayListBaseResponse.getResponse());
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.hideLoadingIndicator();
                        view.showNoInternetLayout();
                    }
                });
    }

    @Override
    public void unsubscribe() {
        disposable.clear();
    }
}
