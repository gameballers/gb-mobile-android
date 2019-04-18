package com.gameball.gameball.views.profile;

import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.model.response.Quest;
import com.gameball.gameball.utils.BasePresenter;

import java.util.ArrayList;

public interface ProfileContract
{
    interface View
    {
        void fillPlayerData(PlayerInfo playerInfo, Level nextLevel);

        void onWithUnlocksLoaded(ArrayList<Game> games, ArrayList<Quest> quests);

        void showLoadingIndicator();

        void hideLoadingIndicator();
    }

    interface Presenter extends BasePresenter
    {
        void getPlayerInfo(boolean fromLocal);

        void getWithUnlocks();
    }
}
