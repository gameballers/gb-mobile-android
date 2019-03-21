package com.moneam.gameball;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.gameball.gameball.GameBallApp;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

            findViewById(R.id.show_profile_btn).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        GameBallApp.getInstance(MainActivity.this).showProfile(MainActivity.this);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

    }
}
