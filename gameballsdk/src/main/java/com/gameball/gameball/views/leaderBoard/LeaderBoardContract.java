package com.gameball.gameball.views.leaderBoard;

import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.BasePresenter;

import java.util.ArrayList;

public interface LeaderBoardContract
{
    interface View
    {
        void fillLeaderBoard(ArrayList<PlayerInfo> leaderBoard);

        void showLoadingIndicator();

        void hideLoadingIndicator();
    }

    interface Presenter extends BasePresenter
    {
        void getLeaderBoard();
    }
}
