package com.gameball.gameball.views.profile;

import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;

import java.util.ArrayList;

public interface ProfileContract
{
    interface View
    {
        void fillPlayerData(PlayerInfo playerInfo, Level nextLevel);

        void fillAchievements(ArrayList<Game> games);

        void showLoadingIndicator();

        void hideLoadingIndicator();
    }

    interface Presenter
    {
        void getPlayerInfo(boolean fromLocal);

        void getWithUnlocks();
    }
}
