package com.gameball.gameball.views.mainContainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.gameball.gameball.views.profile.ProfileFragment;

public class TabsAdapter extends FragmentPagerAdapter
{
    public TabsAdapter(FragmentManager fm)
    {
        super(fm);
    }

    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0:
                new Fragment();
                break;
            case 1:
                new ProfileFragment();
                break;
            case 2:
                new Fragment();
                break;
            case 3:
                new Fragment();
                break;
        }

        return null;
    }

    @Override
    public int getCount()
    {
        return 4;
    }
}
