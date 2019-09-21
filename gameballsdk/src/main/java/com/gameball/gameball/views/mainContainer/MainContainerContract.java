package com.gameball.gameball.views.mainContainer;

import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.utils.BasePresenter;
import com.gameball.gameball.utils.BaseView;

public interface MainContainerContract {

    interface View extends BaseView
    {
        void onProfileInfoLoaded(PlayerAttributes playerAttributes, Level nextLevel);
        void showNoInterNetConnection();
    }

    interface Presenter extends BasePresenter
    {
        void getPlayerInfo();
    }
}
