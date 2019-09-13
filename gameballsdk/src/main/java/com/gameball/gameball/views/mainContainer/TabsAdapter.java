package com.gameball.gameball.views.mainContainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.views.achievements.AchievementsFragment;
import com.gameball.gameball.views.leaderBoard.LeaderBoardFragment;
import com.gameball.gameball.views.notification.NotificationFragment;
import com.gameball.gameball.views.profile.ProfileFragment;
import com.gameball.gameball.views.referral.ReferralFragment;

public class TabsAdapter extends FragmentPagerAdapter {
    private ClientBotSettings clientBotSettings;

    public TabsAdapter(FragmentManager fm, ClientBotSettings clientBotSettings) {
        super(fm);
        this.clientBotSettings = clientBotSettings;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfileFragment();
            case 1:
                if (clientBotSettings.isEnableLeaderboard())
                    return new LeaderBoardFragment();
                else if(clientBotSettings.isReferralOn())
                    return new ReferralFragment();
            case 2:
                return new ReferralFragment();
            case 3:
                return new NotificationFragment();
        }

        return new Fragment();
    }

    @Override
    public int getCount() {
        int count = 1;
        if(clientBotSettings.isReferralOn())
            count ++;
        if(clientBotSettings.isEnableLeaderboard())
            count ++;
        if (clientBotSettings.isEnableNotifications())
            count++;

        return count;
    }
}
