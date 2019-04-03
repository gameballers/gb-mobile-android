package com.gameball.gameball.views.achievementDetails;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.utils.ProgressBarAnimation;
import com.google.gson.Gson;

import java.util.Objects;

public class AchievementDetailsActivity extends AppCompatActivity implements View.OnClickListener
{

    public static final int AMOUNT_BASED = 1;
    public static final int ACTION_BASED = 2;
    public static final int ACTION_AND_AMOUNT_BASED = 3;

    public static final int ACTIVATION_FRIBIES_BASED = 2;
    public static final int ACTIVATION_LEVEL_BASED = 3;



    private ImageView challengeIcon;
    private View notAchievedIndicator;
    private ImageView lockedChallengeIndicator;
    private TextView statusTitle;
    private TextView challengeName;
    private TextView challengeDescription;
    private TextView targetAmountCount;
    private TextView progressTitle;
    private ProgressBar challengeAmountProgress;
    private TextView milestoneDescription;
    private TextView rewardTxt;
    private TextView targetActionCount;
    private ProgressBar challengeActionProgress;
    private ImageView statusIcon;
    private TextView statusDescription;
    private ImageButton backBtn;
    private ConstraintLayout milestoneLayout;

    Game game;
    ClientBotSettings clientBotSettings;
    Animation zoomIn;
    Animation fadeIn;
    Animation translate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement_details);
        initComponents();
        initView();
        prepView();
        setupBotSettings();
        fillView();
    }

    private void initComponents()
    {
        String  gameStr = getIntent().getStringExtra(Constants.GAME_OBJ_KEY);
        game = new Gson().fromJson(gameStr, Game.class);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        zoomIn.setDuration(500);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeIn.setDuration(1000);
        translate = AnimationUtils.loadAnimation(this, R.anim.translate_bottom_to_top);
        translate.setDuration(1000);
        translate.setFillAfter(true);
    }

    private void initView()
    {
        challengeIcon = findViewById(R.id.challenge_icon);
        notAchievedIndicator = findViewById(R.id.not_achieved_indicator);
        statusTitle = findViewById(R.id.status_title);
        lockedChallengeIndicator = findViewById(R.id.locked_challenge_indicator);
        challengeName = findViewById(R.id.challenge_name);
        challengeDescription = findViewById(R.id.challenge_description);
        targetAmountCount = findViewById(R.id.target_amount_count);
        progressTitle = findViewById(R.id.progress_title);
        challengeAmountProgress = findViewById(R.id.milestone_amount_progress);
        challengeAmountProgress.setProgress(1);
        milestoneDescription = findViewById(R.id.milestone_description);
        rewardTxt = findViewById(R.id.mileStone_reward_text);
        targetActionCount = findViewById(R.id.target_action_count);
        challengeActionProgress = findViewById(R.id.milestone_action_progress);
        challengeActionProgress.setProgress(1);
        statusIcon = findViewById(R.id.status_icon);
        statusDescription = findViewById(R.id.status_description);
        backBtn = findViewById(R.id.back_btn);
        milestoneLayout = findViewById(R.id.mileStones_layout);
    }

    private void prepView()
    {
        backBtn.setOnClickListener(this);
    }


    private void fillView()
    {
        challengeName.setText(game.getGameName());
        challengeDescription.setText(game.getDescription());
        ImageDownloader.downloadImage(this, challengeIcon, game.getIcon());
        handleUnlocked();
        applyAnimation();
    }

    private void applyAnimation()
    {
        challengeName.startAnimation(fadeIn);
        statusIcon.startAnimation(fadeIn);
        challengeDescription.startAnimation(fadeIn);
        statusDescription.startAnimation(fadeIn);
    }

    private void setupBotSettings()
    {
        LayerDrawable amountProgress = (LayerDrawable) challengeAmountProgress.getProgressDrawable();
        amountProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
        LayerDrawable actionProgress = (LayerDrawable) challengeActionProgress.getProgressDrawable();
        actionProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
        statusTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        progressTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
    }

    private void handleUnlocked()
    {
        if (!game.isUnlocked())
        {
            lockedChallengeIndicator.setVisibility(View.VISIBLE);
            notAchievedIndicator.setVisibility(View.VISIBLE);
            statusIcon.setImageResource(R.drawable.ic_status_locked);

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
            milestoneLayout.setVisibility(View.VISIBLE);
            lockedChallengeIndicator.setVisibility(View.GONE);
            progressTitle.startAnimation(fadeIn);
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

            rewardTxt.setText(String.format("%d %s | %d %s",game.getRewardFrubies(),
                    getString(R.string.frubies),game.getRewardPoints(),getString(R.string.points)));
        }
    }

    private void showActionProgress()
    {
        challengeActionProgress.setVisibility(View.VISIBLE);
        targetActionCount.setVisibility(View.VISIBLE);
        targetActionCount.setText(game.getTargetActionsCount() + "");
        milestoneDescription.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetActionsCount() - game.getAchievedActionsCount(), ""));

        final ProgressBarAnimation actionProgressBarAnimation = new ProgressBarAnimation(challengeActionProgress,
                0,(int) game.getActionsCompletedPercentage());
        actionProgressBarAnimation.setDuration(700);
        actionProgressBarAnimation.setFillAfter(true);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                challengeActionProgress.startAnimation(actionProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        targetActionCount.startAnimation(fadeIn);
        challengeActionProgress.startAnimation(fadeIn);
        milestoneDescription.startAnimation(fadeIn);
    }

    private void showAmountProgress()
    {
        challengeAmountProgress.setVisibility(View.VISIBLE);
        targetAmountCount.setVisibility(View.VISIBLE);

        String targetAmountStr = "" + game.getTargetAmount();
        if(game.getAmountUnit() != null)
            targetAmountStr += " " + game.getAmountUnit();
        targetAmountCount.setText(targetAmountStr);

        milestoneDescription.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetAmount() - game.getCurrentAmount(), ""));

        if(game.getAmountCompletedPercentage() == 0)
            challengeAmountProgress.setProgress(0);
        final ProgressBarAnimation amountProgressBarAnimation = new ProgressBarAnimation(challengeAmountProgress,
                0,(int) game.getAmountCompletedPercentage());
        amountProgressBarAnimation.setDuration(700);
        amountProgressBarAnimation.setFillAfter(true);


        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                challengeAmountProgress.startAnimation(amountProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        targetAmountCount.startAnimation(fadeIn);
        challengeAmountProgress.startAnimation(fadeIn);
        milestoneDescription.startAnimation(fadeIn);
    }


    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.back_btn)
        {
            onBackPressed();
        }
    }
}
