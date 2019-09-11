package com.gameball.gameball.views.mainContainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.gameball.gameball.views.achievements.AchievementsFragment;
import com.gameball.gameball.views.leaderBoard.LeaderBoardFragment;
import com.gameball.gameball.views.profile.ProfileFragment;
import com.gameball.gameball.views.referral.ReferralFragment;

public class TabsAdapter extends FragmentPagerAdapter {
    private boolean isLeaderboardEnabled;
    private boolean isReferralEnabled;

    public TabsAdapter(FragmentManager fm, boolean isLeaderboardEnabled, boolean isReferralEnabled) {
        super(fm);
        this.isLeaderboardEnabled = isLeaderboardEnabled;
        this.isReferralEnabled = isReferralEnabled;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfileFragment();
            case 1:
                if (isLeaderboardEnabled)
                    return new LeaderBoardFragment();
                else if(isReferralEnabled)
                    return new ReferralFragment();
            case 2:
                return new ReferralFragment();
        }

        return new Fragment();
    }

    @Override
    public int getCount() {
        int count = 1;
        if(isReferralEnabled)
            count ++;
        if(isLeaderboardEnabled)
            count ++;


        return count;
    }
}
