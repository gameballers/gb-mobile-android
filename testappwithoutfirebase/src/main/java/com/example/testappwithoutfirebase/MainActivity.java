package com.example.testappwithoutfirebase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.gameball.gameball.GameBallApp;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GameBallApp.getInstance(this).init("8fdfd2dffd-9mnvhu25d6c3d",
                "SomeGuid16", R.mipmap.ic_launcher);

        findViewById(R.id.btn_show_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GameBallApp.getInstance(MainActivity.this.getApplicationContext())
                        .showProfile(MainActivity.this);
            }
        });

//        navigateToFragment(new MainContainerFragment());
    }

    public void navigateToFragment(Fragment fragment) {
        if (fragment != null) {
            String tag = fragment.getClass().getSimpleName();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);

            fragmentTransaction.replace(R.id.container, fragment, tag);

            fragmentTransaction.addToBackStack(tag);

            fragmentTransaction.commit();
        }
    }
}
