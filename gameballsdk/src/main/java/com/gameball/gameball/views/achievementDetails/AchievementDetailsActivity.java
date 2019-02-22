package com.gameball.gameball.views.achievementDetails;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.network.utils.DownloadImage;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.google.gson.Gson;

public class AchievementDetailsActivity extends AppCompatActivity
{

    public static final int AMOUNT_BASED = 1;
    public static final int ACTION_BASED = 2;
    public static final int ACTION_AND_AMOUNT_BASED = 3;

    public static final int ACTIVATION_FRIBIES_BASED = 2;
    public static final int ACTIVATION_LEVEL_BASED = 3;



    private ImageView challengeIcon;
    private View notAchievedIndicator;
    private ImageView lockedChallengeIndicator;
    private TextView challengeName;
    private TextView challengeDescription;
    private TextView targetAmountCount;
    private TextView progressTitle;
    private ProgressBar challengeAmountProgress;
    private TextView targetAmountDescription;
    private TextView targetActionCount;
    private ProgressBar challengeActionProgress;
    private TextView targetActionDescription;
    private ImageView statusIcon;
    private TextView statusDescription;
    private ImageButton backBtn;
    private View separator;

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
        progressTitle = (TextView) findViewById(R.id.progress_title);
        challengeAmountProgress = (ProgressBar) findViewById(R.id.challenge_amount_progress);
        targetAmountDescription = (TextView) findViewById(R.id.target_amount_description);
        targetActionCount = (TextView) findViewById(R.id.target_action_count);
        challengeActionProgress = (ProgressBar) findViewById(R.id.challenge_action_progress);
        targetActionDescription = (TextView) findViewById(R.id.target_action_description);
        statusIcon = (ImageView) findViewById(R.id.status_icon);
        statusDescription = (TextView) findViewById(R.id.status_description);
        backBtn = (ImageButton) findViewById(R.id.back_btn);
        separator = findViewById(R.id.separator);
    }

    private void fillView()
    {
        challengeName.setText(game.getGameName());
        challengeDescription.setText(game.getDescription());
        ImageDownloader.downloadImage(challengeIcon, BuildConfig.MAIN_HOST + game.getIcon());
        handleUnlocked();

    }

    private void handleUnlocked()
    {
        if (!game.getIsUnlocked())
        {
            lockedChallengeIndicator.setVisibility(View.VISIBLE);
            notAchievedIndicator.setVisibility(View.VISIBLE);
            statusIcon.setImageResource(R.drawable.ic_status_locked);
            progressTitle.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);

            String statusSuffix = "";
            switch (game.getActivationCriteriaTypeId())
            {
                case ACTIVATION_FRIBIES_BASED:
                    statusSuffix  = game.getActivationFrubes() + " " + getString(R.string.frubies);
                    break;
                case ACTIVATION_LEVEL_BASED:
                    statusSuffix = getString(R.string.reach_level)+ " " + game.getActivationLevel();

            }
            statusDescription.setText(String.format("%s %s %s %s",getString(R.string.locked),
                    getString(R.string.you_need_to),statusSuffix,getString(R.string.to_unlock_this_level)));
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

            switch (game.getBehaviorTypeId())
            {
                case ACTION_BASED:
                    showActionProgress();
                    break;
                case AMOUNT_BASED:
                    showAmountProgress();
                    break;
                case ACTION_AND_AMOUNT_BASED:
                    showAmountProgress();
                    showActionProgress();
                    break;
            }
        }
    }

    private void showActionProgress()
    {
        targetActionCount.setVisibility(View.VISIBLE);
        challengeActionProgress.setVisibility(View.VISIBLE);
        targetActionDescription.setVisibility(View.VISIBLE);
        challengeActionProgress.setProgress((int) game.getActionsCompletedPercentage());
        targetActionCount.setText(game.getTargetActionsCount() + "");
        targetActionDescription.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetActionsCount() - game.getAchievedActionsCount(), ""));
    }

    private void showAmountProgress()
    {
        targetAmountCount.setVisibility(View.VISIBLE);
        challengeAmountProgress.setVisibility(View.VISIBLE);
        targetAmountDescription.setVisibility(View.VISIBLE);
        challengeAmountProgress.setProgress((int) game.getAmountCompletedPercentage());
        targetAmountCount.setText(game.getTargetAmount() + "");
        targetAmountDescription.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetAmount() - game.getCurrentAmount(), ""));
    }


}