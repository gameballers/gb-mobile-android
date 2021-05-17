package com.example.testappwithoutfirebase;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gameball.gameball.GameBallApp;
import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private EditText playerCategoryID;

    private GameBallApp gameBallApp;
    private EditText playerIDField;
    private Button registerBtn;
    private EditText challengeApiIdField;
    private EditText apiKeyField;
    private Button addChallengeIdBtn;
    private RecyclerView challengeApiIdsRecyclerview;
    private Button submitActionsBtn;
    private Button btnShowProfile;
    private Button changeLangBtn;
    private Button apiKeyBtn;

    private ChallengeApiIDAdapter adapter;
    private PopupMenu langPopupMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameBallApp = GameBallApp.getInstance(MainActivity.this.getApplicationContext());
        adapter = new ChallengeApiIDAdapter(this, new ArrayList<String>());

        initView();
        prepView();
        daynamicLinkTest();
//        changeLang(Locale.getDefault().getLanguage());


//        gameBallApp.generateOTP(new GenerateOTPBody("5sdfd2dvvd-9mnvhu25d6c3d"),
//                new Callback()
//                {
//                    @Override
//                    public void onSuccess(Object o)
//                    {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e)
//                    {
//
//                    }
//                });
    }

    private void daynamicLinkTest()
    {
        gameBallApp.addReferral(this, getIntent(), new Callback() {
            @Override
            public void onSuccess(Object o) {

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    public void navigateToFragment(Fragment fragment)
    {
        if (fragment != null)
        {
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

    private void initView()
    {
        playerIDField = findViewById(R.id.player_id_filed);
        playerCategoryID = findViewById(R.id.playerCategoryID);
        registerBtn = findViewById(R.id.register_btn);
        challengeApiIdField = findViewById(R.id.challenge_api_id_field);
        addChallengeIdBtn = findViewById(R.id.add_challenge_id_btn);
        challengeApiIdsRecyclerview = findViewById(R.id.challenge_api_ids_recyclerview);
        submitActionsBtn = findViewById(R.id.submit_actions_btn);
        btnShowProfile = findViewById(R.id.btn_show_profile);
        changeLangBtn = findViewById(R.id.change_lang_btn);
        apiKeyField = findViewById(R.id.api_key_field);
        apiKeyBtn = findViewById(R.id.api_key_btn);
    }

    private void prepView()
    {
        challengeApiIdsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        challengeApiIdsRecyclerview.setNestedScrollingEnabled(false);
        challengeApiIdsRecyclerview.setAdapter(adapter);

        registerBtn.setOnClickListener(this);
        addChallengeIdBtn.setOnClickListener(this);
        submitActionsBtn.setOnClickListener(this);
        btnShowProfile.setOnClickListener(this);
        changeLangBtn.setOnClickListener(this);
        apiKeyBtn.setOnClickListener(this);


        langPopupMenu = new PopupMenu(this,changeLangBtn);
        langPopupMenu.getMenu().add("en");
        langPopupMenu.getMenu().add("ar");
        langPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                changeLang(item.getTitle().toString());
                return true;
            }
        });

    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.register_btn:
                registerPlayer();
                break;
            case R.id.btn_show_profile:
                showProfile();
                break;
            case R.id.add_challenge_id_btn:
                challengeApiIdField.setError(null);
                if (!challengeApiIdField.getText().toString().isEmpty())
                    adapter.addChallengeApiId(challengeApiIdField.getText().toString());
                else
                    challengeApiIdField.setError("field cannot be empty");
                break;
            case R.id.submit_actions_btn:
//                if (adapter.getmData().size() > 0)
                    //Todo: addAction test method to be implemented

                    Action action = new Action();

                HashMap<String, Object> metaData = new HashMap<>();
//                metaData.put("Amount", 5000);


                action.addEvent("view_product_page", metaData);

                gameBallApp.addAction(action, new Callback()
                {
                    @Override
                    public void onSuccess(Object o)
                    {

                    }

                    @Override
                    public void onError(Throwable e)
                    {

                    }
                });
                break;
            case R.id.change_lang_btn:
                langPopupMenu.show();
                break;
            case R.id.api_key_btn:
                if(!apiKeyField.getText().toString().trim().isEmpty())
                {
                    gameBallApp.init(apiKeyField.getText().toString(),R.mipmap.ic_launcher, Locale.getDefault().getLanguage());
                }
                else
                    Toast.makeText(this,
                            "Api key cannot be empty",
                            Toast.LENGTH_LONG).show();
                break;

        }
    }

    private void holdRedeemPoints()
    {
//        gameBallApp.holdPoints(new HoldPointBody(10, "08773", "5sdfd2dvvd-9mnvhu25d6c3d"),
//                new Callback<HoldPointsResponse>()
//                {
//                    @Override
//                    public void onSuccess(HoldPointsResponse holdPointsResponse)
//                    {
//                        Log.i("hold_response", holdPointsResponse.getHoldReference());
//                    }
//
//                    @Override
//                    public void onError(Throwable e)
//                    {
//                        Log.i("hold_response", e.getMessage());
//                    }
//                });
    }

    private void showProfile()
    {
        gameBallApp.showProfile(MainActivity.this, null);
    }

    private void registerPlayer()
    {
        if (!playerIDField.getText().toString().trim().isEmpty())
        {

            PlayerAttributes player = new PlayerAttributes.Builder().withDisplayName(playerIDField.getText().toString()).build();

            gameBallApp.registerPlayer(playerIDField.getText().toString().trim(), player,
                        new Callback<PlayerRegisterResponse>()
                        {
                            @Override
                            public void onSuccess(PlayerRegisterResponse playerRegisterResponse)
                            {

                            }

                            @Override
                            public void onError(Throwable e)
                            {

                            }
                        });
        } else
            Toast.makeText(MainActivity.this,
                    "playerID cannot be empty", Toast.LENGTH_SHORT).show();
    }

    private void changeLang(String lang)
    {
        changeLangBtn.setText(lang);

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getResources().getDisplayMetrics());

        gameBallApp.init("19f5b482e8e8497ab480b0eb47892afb",R.mipmap.ic_launcher,lang);
    }
}
