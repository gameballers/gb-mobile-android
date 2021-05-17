package com.moneam.gameball;

import android.app.Application;

import com.gameball.gameball.GameBallApp;

import java.util.Locale;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class GameBallDemoApplication extends Application {
    public static final String ExternalId = "00397ce7-216b-4ce1-a461-dd897c8231bb";

    @Override
    public void onCreate() {
        super.onCreate();

        GameBallApp.getInstance(this).init("8fdfd2dffd-9mnvhu25d6c3d", R.mipmap.ic_launcher, Locale.getDefault().getLanguage());

        // TODO: 8/23/2018
    }
}
