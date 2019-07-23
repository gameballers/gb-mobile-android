package com.gameball.gameball.views.mainContainer;

import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.BasePresenter;
import com.gameball.gameball.utils.BaseView;

public interface MainContainerContract {

    interface View extends BaseView
    {
        void onProfileInfoLoaded(PlayerInfo playerInfo, Level nextLevel);
    }

    interface Presenter extends BasePresenter
    {
        void getPlayerInfo();
    }
}
