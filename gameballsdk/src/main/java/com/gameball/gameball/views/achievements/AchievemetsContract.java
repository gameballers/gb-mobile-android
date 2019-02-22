package com.gameball.gameball.views.achievements;

import com.gameball.gameball.model.response.Game;

import java.util.ArrayList;

public interface AchievemetsContract
{
    interface View
    {
        void fillAchievements(ArrayList<Game> games);

        void showLoadingIndicator();

        void hideLoadingIndicator();

    }

    interface Presenter
    {
        void getAchievements();
    }
}
