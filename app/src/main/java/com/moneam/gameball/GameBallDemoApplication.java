package com.moneam.gameball;

import android.app.Application;

import com.gameball.gameball.GameBallApp;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class GameBallDemoApplication extends Application {
    public static final String ExternalId = "00397ce7-216b-4ce1-a461-dd897c8231bb";

    @Override
    public void onCreate() {
        super.onCreate();

        GameBallApp.getInstance(this).init(2, ExternalId, R.mipmap.ic_launcher);

        // TODO: 8/23/2018
    }
}
