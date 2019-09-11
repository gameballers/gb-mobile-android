package com.gameball.gameball.views.referral;

import com.gameball.gameball.model.response.Game;

import java.util.ArrayList;

public interface ReferralContract
{
    interface View
    {
        void onReferralChallengesFiltered(ArrayList<Game> games);
    }

    interface Presenter
    {
        void getReferralChallenges();
    }
}
