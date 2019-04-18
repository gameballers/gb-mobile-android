package com.example.testappwithoutfirebase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gameball.gameball.GameBallApp;
import com.gameball.gameball.model.request.Action;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private EditText playerCategoryID;

    private GameBallApp gameBallApp;
    private EditText playerIDField;
    private Button registerBtn;
    private EditText challengeApiIdField;
    private Button addChallengeIdBtn;
    private RecyclerView challengeApiIdsRecyclerview;
    private Button submitActionsBtn;
    private Button btnShowProfile;

    private  ChallengeApiIDAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameBallApp = GameBallApp.getInstance(MainActivity.this.getApplicationContext());
        adapter = new ChallengeApiIDAdapter(this, new ArrayList<String>());

        initView();
        prepView();
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
                if(!challengeApiIdField.getText().toString().isEmpty())
                    adapter.addChallengeApiId(challengeApiIdField.getText().toString());
                else
                    challengeApiIdField.setError("field cannot be empty");
                break;
            case R.id.submit_actions_btn:
                addAction();
                break;

        }
    }

    private void showProfile()
    {
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

    private void registerPlayer()
    {
        if (!playerIDField.getText().toString().trim().isEmpty())
        {
            if (!playerCategoryID.getText().toString().trim().isEmpty())
                gameBallApp.registerPlayer(playerIDField.getText().toString().trim(),
                        Integer.parseInt(playerCategoryID.getText().toString().trim()));
            else
                gameBallApp.registerPlayer(playerIDField.getText().toString());
        } else
            Toast.makeText(MainActivity.this,
                    "playerID cannot be empty", Toast.LENGTH_SHORT).show();
    }

    private void addAction()
    {
        ArrayList<String> challengeApiIds = adapter.getmData();

        Action action;

        if(challengeApiIds.size() > 1)
            action = new Action(challengeApiIds);
        else
            action = new Action(challengeApiIds.get(0), null);

        gameBallApp.addAction(action);
    }
}
