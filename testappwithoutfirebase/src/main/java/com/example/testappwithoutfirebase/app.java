package com.example.testappwithoutfirebase;

import android.app.Application;
import android.content.Context;

import com.gameball.gameball.GameBallApp;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 2/5/2019.
 */
public class app extends Application {
    public static final String ExternalId = "00397ce7-216b-4ce1-a461-dd897c8231bb";

    @Override
    public void onCreate() {
        super.onCreate();

        GameBallApp.getInstance(getApplicationContext()).init("aa3672f6e9ca41a1b2b54543bd7753c0",
                "badr",3, R.mipmap.ic_launcher);
    }
}
