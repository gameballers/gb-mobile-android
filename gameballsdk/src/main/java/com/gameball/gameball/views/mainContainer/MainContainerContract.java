package com.gameball.gameball.views.mainContainer;

import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.BaseView;

public interface MainContainerContract {

    interface View extends BaseView
    {
        void setPlayerName(String name);
        void setPlayerEmail(String email);

        void onProfileInfoLoaded(PlayerInfo playerInfo);

    }

    interface Presenter
    {
        void getPlayerInfo();
    }
}
