package com.example.testappwithoutfirebase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.gameball.gameball.GameBallApp;
import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.network.api.GameBallApi;

public class MainActivity extends AppCompatActivity {

    EditText playerIDField;

    private GameBallApp gameBallApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerIDField = findViewById(R.id.player_id_filed);

        gameBallApp = GameBallApp.getInstance(MainActivity.this.getApplicationContext());

        findViewById(R.id.register_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!playerIDField.getText().toString().trim().isEmpty())
                {
                    gameBallApp.registerPlayer(playerIDField.getText().toString());
                }
            }
        });

        findViewById(R.id.btn_show_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    gameBallApp.showProfile(MainActivity.this);
                } catch (Exception e)
                {
                    Toast.makeText(MainActivity.this, "enter player ID then hit register," +
                                    "and then try to show profile"
                            , Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.action_3).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Action action = new Action("3",15);
                GameBallApp.getInstance(MainActivity.this).AddAction(action);
            }
        });

        findViewById(R.id.action_1004).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Action action = new Action("1004");
                GameBallApp.getInstance(MainActivity.this).AddAction(action);
            }
        });

//        navigateToFragment(new MainContainerFragment());
        findViewById(R.id.btn_show_notification).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameBallApp.showNotification();
            }
        });

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
