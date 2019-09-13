package com.gameball.gameball.views.notification;

import com.gameball.gameball.model.response.Notification;
import com.gameball.gameball.utils.BasePresenter;
import com.gameball.gameball.utils.BaseView;

import java.util.ArrayList;

public interface NotificationsContract {
    interface View extends BaseView {
        void onNotificationsLoaded(ArrayList<Notification> notifications);
    }

    interface Presenter extends BasePresenter {
        void getNotificationHistory();
    }
}
