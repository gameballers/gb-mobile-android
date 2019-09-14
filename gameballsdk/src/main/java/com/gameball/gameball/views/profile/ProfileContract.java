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
        void onWithUnlocksLoaded(ArrayList<Game> games);

        void showLoadingIndicator();

        void hideLoadingIndicator();
        void showNoInternetConnectionLayout();
    }

    interface Presenter extends BasePresenter
    {
        void getWithUnlocks();
    }
}
