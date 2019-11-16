package com.example.testappwithoutfirebase;

import android.app.Application;

import com.gameball.gameball.GameBallApp;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 2/5/2019.
 */
public class app extends Application {
    public static final String ExternalId = "00397ce7-216b-4ce1-a461-dd897c8231bb";

    @Override
    public void onCreate() {
        super.onCreate();

        GameBallApp.getInstance(getApplicationContext()).init("0a95ff267d9746668a63aa7986e5fcf0",
                R.mipmap.ic_launcher);
    }
}
