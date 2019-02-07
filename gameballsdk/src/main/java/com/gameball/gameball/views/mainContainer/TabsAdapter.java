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
                return new Fragment();
            case 1:
                return new ProfileFragment();
            case 2:
                return new Fragment();
            case 3:
                return new Fragment();
        }

        return null;
    }

    @Override
    public int getCount()
    {
        return 4;
    }
}
