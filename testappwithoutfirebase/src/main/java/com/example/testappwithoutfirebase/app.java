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
        GameBallApp.getInstance(this).init("8fdfd2dffd-9mnvhu25d6c3d",
                "badr@badr.com",3, R.mipmap.ic_launcher);
    }
}
