package com.gameball.gameball.views.mainContainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.gameball.gameball.views.achievements.AchievementsFragment;
import com.gameball.gameball.views.leaderBoard.LeaderBoardFragment;
import com.gameball.gameball.views.profile.ProfileFragment;

public class TabsAdapter extends FragmentPagerAdapter {
    private boolean enableLeaderboard;

    public TabsAdapter(FragmentManager fm, boolean enableLeaderboard) {
        super(fm);
        this.enableLeaderboard = enableLeaderboard;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfileFragment();
            case 1:
                if (enableLeaderboard)
                    return new LeaderBoardFragment();
            case 2:
                return new AchievementsFragment();
            case 3:
                return new Fragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        if(enableLeaderboard)
            return 2;
        else
            return 1;
    }
}
