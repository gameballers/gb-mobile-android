package com.gameball.gameball.views.challengeDetails;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.DisplayUtils;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.utils.ProgressBarAnimation;
import com.google.gson.Gson;

public class ChallengeDetailsActivity extends AppCompatActivity implements View.OnClickListener
{

    public static final int AMOUNT_BASED = 1;
    public static final int ACTION_BASED = 2;
    public static final int ACTION_AND_AMOUNT_BASED = 3;
    public static final int HIGH_SCORE_BASED = 4;

    public static final int ACTIVATION_FRUBIES_BASED = 2;
    public static final int ACTIVATION_LEVEL_BASED = 3;


    private ImageView challengeIcon;
    private View notAchievedIndicator;
    private ImageView lockedChallengeIndicator;
    private TextView challengeName;
    private TextView challengeDescription;

    //mileStone views
    private ConstraintLayout milestoneLayout;
    private TextView milestoneTitle;
    private RecyclerView milestonesRecyclerView;
    private TextView milestoneTargetAmountCount;
    private TextView milestoneTargetActionCount;
    private ProgressBar milestoneAmountProgress;
    private ProgressBar milestoneActionProgress;
    private TextView milestoneDescription;
    private TextView milestoneRewardTxt;

    //normal challenge views
    private RelativeLayout challengeLayout;
    private TextView progressTitle;
    private TextView challengeTargetAmountCount;
    private TextView challengeTargetActionCount;
    private TextView challengeAmountDescription;
    private TextView challengeActionDescription;
    private ProgressBar challengeAmountProgress;
    private ProgressBar challengeActionProgress;
    private TextView challengeRewardTxt;

    //high score views
    private RelativeLayout highScoreLayout;
    private TextView highScoreTitle;
    private TextView highScoreValue;

    //status views
    private LinearLayout statusLayout;
    private TextView statusTitle;
    private ImageView statusIcon;
    private TextView statusDescription;

    private ImageButton backBtn;


    Game game;
    ClientBotSettings clientBotSettings;
    MilestonesAdapter adapter;
    Animation zoomIn;
    Animation fadeIn;
    Animation translate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_details);
        initComponents();
        initView();
        prepView();
        setupBotSettings();
        fillView();
    }

    private void initComponents()
    {
        String gameStr = getIntent().getStringExtra(Constants.GAME_OBJ_KEY);
        game = new Gson().fromJson(gameStr, Game.class);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        adapter = new MilestonesAdapter(this, game.getMilestones());
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
        milestoneTargetAmountCount = findViewById(R.id.milestone_target_amount_count);
        challengeTargetAmountCount = findViewById(R.id.challenge_target_amount_count);
        milestoneTitle = findViewById(R.id.milestone_title);
        milestoneAmountProgress = findViewById(R.id.milestone_amount_progress);
        milestoneAmountProgress.setProgress(1);
        milestoneDescription = findViewById(R.id.milestone_description);
        milestoneRewardTxt = findViewById(R.id.mileStone_reward_text);
        challengeRewardTxt = findViewById(R.id.challenge_reward_txt);
        milestoneTargetActionCount = findViewById(R.id.milestone_target_action_count);
        challengeTargetActionCount = findViewById(R.id.challenge_target_action_count);
        milestoneActionProgress = findViewById(R.id.milestone_action_progress);
        milestoneActionProgress.setProgress(1);
        statusIcon = findViewById(R.id.status_icon);
        statusDescription = findViewById(R.id.status_description);
        backBtn = findViewById(R.id.back_btn);
        challengeAmountDescription = findViewById(R.id.challenge_amount_description);
        challengeActionDescription = findViewById(R.id.challenge_action_description);
        milestoneLayout = findViewById(R.id.mileStones_layout);
        milestonesRecyclerView = findViewById(R.id.milestones_recyclerView);
        challengeLayout = findViewById(R.id.challenge_layout);
        progressTitle = findViewById(R.id.progress_title);
        challengeAmountProgress = findViewById(R.id.challenge_amount_progress);
        challengeAmountProgress.setProgress(1);
        challengeActionProgress = findViewById(R.id.challenge_action_progress);
        challengeActionProgress.setProgress(1);
        highScoreLayout = findViewById(R.id.high_score_layout);
        highScoreTitle = findViewById(R.id.high_score_title);
        highScoreValue = findViewById(R.id.high_score_value);
        statusLayout = findViewById(R.id.status_layout);
    }


    private void prepView()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            DisplayUtils.statusBarColorToSolid(this, "#000000");
        }
        milestonesRecyclerView.setNestedScrollingEnabled(false);
        milestonesRecyclerView.setHasFixedSize(true);
        milestonesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        milestonesRecyclerView.setAdapter(adapter);
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
        LayerDrawable amountProgress = (LayerDrawable) milestoneAmountProgress.getProgressDrawable();
        amountProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
        LayerDrawable actionProgress = (LayerDrawable) milestoneActionProgress.getProgressDrawable();
        actionProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);

        amountProgress = (LayerDrawable) challengeAmountProgress.getProgressDrawable();
        amountProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
        actionProgress = (LayerDrawable) challengeActionProgress.getProgressDrawable();
        actionProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);

        statusTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        milestoneTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        progressTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        highScoreTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));

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
                case ACTIVATION_FRUBIES_BASED:
                    statusSuffix = game.getActivationFrubes() + " " + getString(R.string.frubies);
                    break;
                case ACTIVATION_LEVEL_BASED:
                    statusSuffix = getString(R.string.reach_level) + " " + game.getActivationLevel();

            }
            statusDescription.setText(String.format("%s %s %s %s", getString(R.string.locked),
                    getString(R.string.you_need_to), statusSuffix, getString(R.string.to_unlock_this_level)));
        } else
        {
            lockedChallengeIndicator.setVisibility(View.GONE);
            if (game.getAchievedCount() > 0 || game.getBehaviorTypeId() == 5)
            {
                notAchievedIndicator.setVisibility(View.GONE);
                statusIcon.setImageResource(R.drawable.ic_status_achieved);
                statusDescription.setText(String.format("%s (%d)", getString(R.string.achieved), game.getAchievedCount()));
            } else
            {
                notAchievedIndicator.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_status_keep_going);
                statusDescription.setText(R.string.keep_going);
            }

            if (game.getMilestones().size() > 0)
            {
                milestoneLayout.setVisibility(View.VISIBLE);
                milestoneTitle.startAnimation(fadeIn);
            } else if(game.getBehaviorTypeId() == HIGH_SCORE_BASED)
            {
                highScoreValue.setText(String.format("%d %s",game.getHighScore(),
                        game.getAmountUnit()));
                highScoreLayout.setVisibility(View.VISIBLE);
                statusLayout.setVisibility(View.GONE);
            }
            else
            {
                challengeLayout.setVisibility(View.VISIBLE);
                progressTitle.startAnimation(fadeIn);
            }

            switch (game.getBehaviorTypeId())
            {
                case ACTION_BASED:
                    if (game.getMilestones().size() > 0)
                        showActionProgress(milestoneActionProgress, milestoneTargetActionCount,
                                milestoneDescription);
                    else
                        showActionProgress(challengeActionProgress, challengeTargetActionCount,
                                challengeActionDescription);
                    break;
                case AMOUNT_BASED:
                    if (game.getMilestones().size() > 0)
                        showAmountProgress(milestoneAmountProgress, milestoneTargetAmountCount,
                                milestoneDescription);
                    else
                        showAmountProgress(challengeAmountProgress, challengeTargetAmountCount,
                                challengeAmountDescription);
                    break;
                case ACTION_AND_AMOUNT_BASED:
                    if (game.getMilestones().size() > 0)
                    {
                        showAmountProgress(milestoneAmountProgress, milestoneTargetAmountCount,
                                milestoneDescription);
                        showActionProgress(milestoneActionProgress, milestoneTargetActionCount,
                                milestoneDescription);
                    } else
                    {
                        showAmountProgress(challengeAmountProgress, challengeTargetAmountCount,
                                challengeAmountDescription);
                        showActionProgress(challengeActionProgress, challengeTargetActionCount,
                                challengeActionDescription);
                    }
                    break;
            }

            String challengeRewardStr = String.format("%d %s | %d %s", game.getRewardFrubies(),
                    getString(R.string.frubies), game.getRewardPoints(), getString(R.string.points));

            if (game.getMilestones().size() > 0)
            {
                milestoneRewardTxt.setText(challengeRewardStr);
            } else
            {
                challengeRewardTxt.setVisibility(View.VISIBLE);
                milestoneRewardTxt.setVisibility(View.GONE);
                challengeRewardTxt.setText(challengeRewardStr);
            }
        }
    }

    private void showActionProgress(final ProgressBar progressBar, TextView actionCountTxt, TextView description)
    {
        progressBar.setVisibility(View.VISIBLE);
        actionCountTxt.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);

        actionCountTxt.setText(game.getTargetActionsCount() + "");
        description.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetActionsCount() - game.getAchievedActionsCount(), ""));

        if (game.getAmountCompletedPercentage() == 0)
            progressBar.setProgress(0);

        final ProgressBarAnimation actionProgressBarAnimation = new ProgressBarAnimation(progressBar,
                0, (int) game.getActionsCompletedPercentage());
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
                progressBar.startAnimation(actionProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        actionCountTxt.startAnimation(fadeIn);
        progressBar.startAnimation(fadeIn);
        description.startAnimation(fadeIn);
    }

    private void showAmountProgress(final ProgressBar progressBar, TextView amountCountTxt, TextView description)
    {
        progressBar.setVisibility(View.VISIBLE);
        amountCountTxt.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);

        String targetAmountStr = "" + game.getTargetAmount();
        if (game.getAmountUnit() != null)
            targetAmountStr += " " + game.getAmountUnit();
        amountCountTxt.setText(targetAmountStr);

        description.setText(String.format("only %d %s remaining to achive this challenge",
                game.getTargetAmount() - game.getCurrentAmount(), ""));

        if (game.getAmountCompletedPercentage() == 0)
            progressBar.setProgress(0);
        final ProgressBarAnimation amountProgressBarAnimation = new ProgressBarAnimation(progressBar,
                0, (int) game.getAmountCompletedPercentage());
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
                progressBar.startAnimation(amountProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        amountCountTxt.startAnimation(fadeIn);
        progressBar.startAnimation(fadeIn);
        description.startAnimation(fadeIn);
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
