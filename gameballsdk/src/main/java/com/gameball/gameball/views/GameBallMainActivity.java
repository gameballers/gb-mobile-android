package com.gameball.gameball.views;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.gameball.gameball.R;
import com.gameball.gameball.views.mainContainer.MainContainerFragment;

public class GameBallMainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameball_main);

        navigateToFragment(new MainContainerFragment());

    }

    public void navigateToFragment(Fragment fragment)
    {
        if (fragment != null /*&& !is(fragment.getClass().getSimpleName())*/)
        {
            String tag = fragment.getClass().getSimpleName();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);

            fragmentTransaction.replace(R.id.main_activity_container, fragment, tag);

            fragmentTransaction.commit();
        }
    }
}