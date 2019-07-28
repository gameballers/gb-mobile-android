package com.gameball.gameball.views.referral;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.gameball.gameball.model.response.Game;

import io.reactivex.functions.Predicate;

public class ReferralChallengesFilter implements Predicate<Game>
{
    @Override
    public boolean test(Game game) throws Exception
    {
        return game.isReferral();
    }
}
