package com.gameball.gameball.views.achievementDetails;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.network.utils.DownloadImage;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.google.gson.Gson;

public class AchievementDetailsActivity extends AppCompatActivity
{

    private ImageView challengeIcon;
    private View notAchievedIndicator;
    private ImageView lockedChallengeIndicator;
    private TextView challengeName;
    private TextView challengeDescription;
    private TextView targetAmountCount;
    private ProgressBar challengeAmountProgress;
    private TextView targetAmountDescription;
    private TextView targetActionCount;
    private ProgressBar challengeActionProgress;
    private TextView targetActionDescription;
    private ImageView statusIcon;
    private TextView statusDescription;
    private ImageButton backBtn;

    Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement_details);
        initComponents();
        initView();
        fillView();
    }

    private void initComponents()
    {
        String  gameStr = getIntent().getStringExtra(Constants.GAME_OBJ_KEY);
        game = new Gson().fromJson(gameStr, Game.class);
    }

    private void initView()
    {
        challengeIcon = (ImageView) findViewById(R.id.challenge_icon);
        notAchievedIndicator = (View) findViewById(R.id.not_achieved_indicator);
        lockedChallengeIndicator = (ImageView) findViewById(R.id.locked_challenge_indicator);
        challengeName = (TextView) findViewById(R.id.challenge_name);
        challengeDescription = (TextView) findViewById(R.id.challenge_description);
        targetAmountCount = (TextView) findViewById(R.id.target_amount_count);
        challengeAmountProgress = (ProgressBar) findViewById(R.id.challenge_amount_progress);
        targetAmountDescription = (TextView) findViewById(R.id.target_amount_description);
        targetActionCount = (TextView) findViewById(R.id.target_action_count);
        challengeActionProgress = (ProgressBar) findViewById(R.id.challenge_action_progress);
        targetActionDescription = (TextView) findViewById(R.id.target_action_description);
        statusIcon = (ImageView) findViewById(R.id.status_icon);
        statusDescription = (TextView) findViewById(R.id.status_description);
        backBtn = (ImageButton) findViewById(R.id.back_btn);
    }

    private void fillView()
    {
        challengeName.setText(game.getGameName());
        challengeDescription.setText(game.getDescription());
        ImageDownloader.downloadImage(challengeIcon,game.getIcon());
        handleUnlocked();

    }

    private void handleUnlocked()
    {
        if (!game.getIsUnlocked())
        {
            lockedChallengeIndicator.setVisibility(View.VISIBLE);
            notAchievedIndicator.setVisibility(View.VISIBLE);
            statusIcon.setImageResource(R.drawable.ic_status_locked);
            statusDescription.setText(R.string.locked);
        }
        else
        {
            lockedChallengeIndicator.setVisibility(View.GONE);
            if(game.getAchievedCount() > 0)
            {
                notAchievedIndicator.setVisibility(View.GONE);
                statusIcon.setImageResource(R.drawable.ic_status_achieved);
                statusDescription.setText(String.format("%s (%d)",getString(R.string.achieved), game.getAchievedCount()));
            }
            else
            {
                notAchievedIndicator.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_status_keep_going);
                statusDescription.setText(R.string.keep_going);
            }
        }
    }


}
