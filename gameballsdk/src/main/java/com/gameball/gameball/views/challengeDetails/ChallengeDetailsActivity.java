package com.gameball.gameball.views.challengeDetails;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

import java.util.Locale;

public class ChallengeDetailsActivity extends AppCompatActivity implements View.OnClickListener
{

    public static final int AMOUNT_BASED = 1;
    public static final int ACTION_BASED = 2;
    public static final int ACTION_AND_AMOUNT_BASED = 3;
    public static final int HIGH_SCORE_BASED = 4;
    public static final int UPON_LOGIN = 5;
    public static final int NON_CUMULATIVE_AMOUNT_BASED = 6;
    public static final int BIRTHDAY = 7;
    public static final int JOIN_ANNIVERSARY = 8;
    public static final int EVENT_BASED = 9;

    public static final int ACTIVATION_FRUBIES_BASED = 2;
    public static final int ACTIVATION_LEVEL_BASED = 3;


    private ImageView challengeIcon;
    private View notAchievedIndicator;
    private ImageView lockedChallengeIndicator;
    private TextView challengeName;
    private TextView challengeDescription;


    //normal challenge views
    private RelativeLayout challengeLayout;
    private TextView progressTitle;
    private TextView challengeTargetEventCount;
    private TextView challengeEventDescription;
    private ProgressBar challengeEventProgress;
    private TextView challengeRewardTxt;

    //high score views
    private RelativeLayout highScoreLayout;
    private TextView highScoreTitle;
    private TextView highScoreValue;
    private TextView isRepeatableHighScoreText;

    //status views
    private LinearLayout statusLayout;
    private TextView statusTitle;
    private ImageView statusIcon;
    private TextView status;
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
        adapter = new MilestonesAdapter(this, game.getMilestones(),game.getBehaviorTypeId());
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
        challengeTargetEventCount = findViewById(R.id.challenge_target_event_count);
        challengeRewardTxt = findViewById(R.id.challenge_reward_txt);
        statusIcon = findViewById(R.id.status_icon);
        status = findViewById(R.id.status_description);
        statusDescription = findViewById(R.id.achieved_count);
        backBtn = findViewById(R.id.back_btn);
        challengeEventDescription = findViewById(R.id.challenge_event_description);
        challengeLayout = findViewById(R.id.challenge_layout);
        progressTitle = findViewById(R.id.progress_title);
        challengeEventProgress = findViewById(R.id.challenge_event_progress);
        challengeEventProgress.setProgress(1);
        highScoreLayout = findViewById(R.id.high_score_layout);
        highScoreTitle = findViewById(R.id.high_score_title);
        highScoreValue = findViewById(R.id.high_score_value);
        statusLayout = findViewById(R.id.status_layout);
        isRepeatableHighScoreText = findViewById(R.id.is_repeatable_high_score_txt);
    }


    private void prepView()
    {
        DisplayUtils.statusBarColorToSolid(this, clientBotSettings.getBotMainColor());

        backBtn.setOnClickListener(this);
    }


    private void applyAnimation()
    {
        challengeName.startAnimation(fadeIn);
        statusIcon.startAnimation(fadeIn);
        challengeDescription.startAnimation(fadeIn);
        status.startAnimation(fadeIn);
    }

    private void setupBotSettings()
    {
        LayerDrawable eventProgress = (LayerDrawable) challengeEventProgress.getProgressDrawable();
        eventProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);

        statusTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        progressTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        highScoreTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));

    }

    private void fillView()
    {
        if(game.getGameName() != null)
            challengeName.setText(game.getGameName());
        challengeDescription.setText(game.getDescription());
        ImageDownloader.downloadImage(this, challengeIcon, game.getIcon());
        setupView();
        applyAnimation();
    }

    private void setupView()
    {
        if (!game.isUnlocked())
        {
            setupLockedStatus();
        } else
        {
            lockedChallengeIndicator.setVisibility(View.GONE);

            if (isChallengeAchieved())
            {
                setupAchievedStatus();
            }
            else
            {
                setupNotAchievedStatus();
            }

            setupViewsByBehaviourTypeId();
        }
    }

    private boolean isChallengeAchieved()
    {
        return game.getAchievedCount() > 0 || game.getBehaviorTypeId() == UPON_LOGIN ||
                (game.getBehaviorTypeId() == HIGH_SCORE_BASED && game.getHighScore() != null &&
                        game.getHighScore() > 0);
    }

    private void setupLockedStatus()
    {
        lockedChallengeIndicator.setVisibility(View.VISIBLE);
        notAchievedIndicator.setVisibility(View.VISIBLE);
        statusIcon.setImageResource(R.drawable.ic_status_locked);

        String statusPrefix = String.format(Locale.getDefault(),"%s %d %s",
                getString(R.string.reach_level), game.getActivationLevel(),
                getString(R.string.to_unlock_this_challenge));

        status.setText(getResources().getString(R.string.locked));
        statusDescription.setText(statusPrefix);

        String challengeRewardStr = String.format(Locale.getDefault(),
                "%d %s | %d %s", game.getRewardFrubies(), clientBotSettings.getWalletPointsName(),
                game.getRewardPoints(), clientBotSettings.getRankPointsName());
        challengeRewardTxt.setText(challengeRewardStr);
    }

    private void setupAchievedStatus()
    {
        notAchievedIndicator.setVisibility(View.GONE);
        statusIcon.setImageResource(R.drawable.ic_status_achieved);
        status.setText(String.format(Locale.getDefault(),
                "%s", getString(R.string.achieved)));
        statusDescription.setText(String.format(Locale.getDefault(), "%d time(s)",game.getAchievedCount()));
    }

    private void setupNotAchievedStatus()
    {
        notAchievedIndicator.setVisibility(View.VISIBLE);
        statusIcon.setImageResource(R.drawable.ic_status_keep_going);
        status.setText(R.string.keep_going);
    }

    private void setupViewsByBehaviourTypeId()
    {
        String challengeRewardStr = String.format(Locale.getDefault(),
                "%d %s | %d %s", game.getRewardFrubies(), clientBotSettings.getWalletPointsName(),
                game.getRewardPoints(), clientBotSettings.getRankPointsName());
        challengeRewardTxt.setText(challengeRewardStr);
        challengeRewardTxt.startAnimation(fadeIn);

        if (game.getBehaviorTypeId() == HIGH_SCORE_BASED)
        {
            if(game.getHighScore() != null)
            {
                highScoreValue.setText(String.format(Locale.getDefault(), "%d %s",
                        game.getHighScore(), game.getAmountUnit()));
                highScoreLayout.setVisibility(View.VISIBLE);
                if(game.isRepeatable())
                    isRepeatableHighScoreText.setVisibility(View.VISIBLE);

            }
        } else
        {
            if (isHideProgressLayout())
                challengeLayout.setVisibility(View.GONE);
            else
            {
                challengeLayout.setVisibility(View.VISIBLE);
                setupProgressbarBehaviour();
            }

            progressTitle.startAnimation(fadeIn);
        }
    }

    private boolean isHideProgressLayout()
    {
        int behaviorTypeId = game.getBehaviorTypeId();
        return behaviorTypeId == BIRTHDAY || behaviorTypeId == JOIN_ANNIVERSARY
                || behaviorTypeId == UPON_LOGIN || (game.isReferral() && isChallengeAchieved())
                || (!game.isRepeatable() && isChallengeAchieved());
    }

    private void setupProgressbarBehaviour()
    {
            switch (game.getBehaviorTypeId())
        {
            case EVENT_BASED:
                    showEventProgress(challengeEventProgress, challengeTargetEventCount,
                            challengeEventDescription);
                break;
        }
    }

    private void showEventProgress(final ProgressBar progressBar, TextView eventCountTxt, TextView description)
    {
        progressBar.setVisibility(View.VISIBLE);
        eventCountTxt.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);

        String targetEventStr = String.format(Locale.getDefault(),
                "%d", game.getTargetAmount());
        if (game.isReferral() && game.getAmountUnit() != null)
            targetEventStr += " " + game.getAmountUnit();
        else
            eventCountTxt.setVisibility(View.GONE);
        eventCountTxt.setText(targetEventStr);

        if(game.isReferral())
            description.setText(String.format(Locale.getDefault(),
                "%d friend(s) remaining to achieve this badge",
                game.getTargetAmount() - game.getCurrentAmount(), ""));
        else
            description.setText(getString(R.string.track_your_progress));

        if (game.getAmountCompletedPercentage() == 0)
            progressBar.setProgress(0);
        final ProgressBarAnimation eventProgressBarAnimation = new ProgressBarAnimation(progressBar,
                0, (int) game.getCompletionPercentage());
        eventProgressBarAnimation.setDuration(700);
        eventProgressBarAnimation.setFillAfter(true);


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
                progressBar.startAnimation(eventProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        eventCountTxt.startAnimation(fadeIn);
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
