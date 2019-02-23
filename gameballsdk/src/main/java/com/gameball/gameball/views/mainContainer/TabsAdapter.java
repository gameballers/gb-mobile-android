package com.gameball.gameball.views.mainContainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.gameball.gameball.views.achievements.AchievementsFragment;
import com.gameball.gameball.views.leaderBoard.LeaderBoardFragment;
import com.gameball.gameball.views.profile.ProfileFragment;

public class TabsAdapter extends FragmentPagerAdapter {
    public TabsAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfileFragment();
            case 1:
                return new AchievementsFragment();
            case 2:
                return new LeaderBoardFragment();
            case 3:
                return new Fragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
