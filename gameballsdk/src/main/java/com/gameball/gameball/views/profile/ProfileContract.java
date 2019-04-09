package com.gameball.gameball.views.profile;

import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;

import java.util.ArrayList;

public interface ProfileContract
{
    interface View
    {
        void fillPlayerData(PlayerDetailsResponse playerDetails , Level nextLevel);

        void fillAchievements(ArrayList<Game> games);

        void showLoadingIndicator();

        void hideLoadingIndicator();
    }

    interface Presenter
    {
        void getPlayerDetails();

        void getWithUnlocks();
    }
}
