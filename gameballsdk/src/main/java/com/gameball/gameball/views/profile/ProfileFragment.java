package com.gameball.gameball.views.profile;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.utils.ProgressBarAnimation;

import java.util.ArrayList;

public class ProfileFragment extends Fragment  implements ProfileContract.View
{
    View rootView;

    private ImageView levelLogo;
    private TextView levelName;
    private ProgressBar levelProgress;
    private TextView frubiesForNextLevel;
    private TextView currentFrubiesValue;
    private TextView currentPointsValue;
    private TextView achievemetTitle;
    private RecyclerView achievementsRecyclerView;
    private ProgressBar profileLoadingIndicator;
    private ProgressBar achievementsLoadingIndicator;
    private View profileLoadingIndicatorBg;
    private ConstraintLayout frubiesAndPointsContainer;

    AchievementsAdapter achievementsAdapter;
    ProfileContract.Presenter presenter;
    ClientBotSettings clientBotSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        initView();
        setupBotSettings();
        prepView();
        presenter.getPlayerDetails();
        presenter.getWithUnlocks();
        return rootView;
    }

    private void initComponents() {
        achievementsAdapter = new AchievementsAdapter(getContext(), new ArrayList<Game>());
        presenter = new ProfilePresenter(getContext(), this);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    private void initView() {
        levelLogo = rootView.findViewById(R.id.level_logo);
        levelName = rootView.findViewById(R.id.level_name);
        levelProgress = rootView.findViewById(R.id.level_progress);
        frubiesForNextLevel = rootView.findViewById(R.id.frubies_for_next_level);
        currentFrubiesValue = rootView.findViewById(R.id.current_frubies_value);
        currentPointsValue = rootView.findViewById(R.id.current_points_value);
        achievemetTitle = rootView.findViewById(R.id.achievements_title);
        achievementsRecyclerView = rootView.findViewById(R.id.achievements_recyclerView);
        profileLoadingIndicator = rootView.findViewById(R.id.profile_data_loading_indicator);
        profileLoadingIndicatorBg = rootView.findViewById(R.id.profile_data_loading_indicator_bg);
        achievementsLoadingIndicator = rootView.findViewById(R.id.achievements_loading_indicator);
        frubiesAndPointsContainer = rootView.findViewById(R.id.frubies_and_points_container);
    }

    private void setupBotSettings()
    {
        LayerDrawable progressDrawable = (LayerDrawable) levelProgress.getProgressDrawable();
        progressDrawable.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
    }

    private void applyAnimation(PlayerDetailsResponse playerDetails, Level nextLevel)
    {
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeIn.setDuration(700);
        Animation zoomInX = AnimationUtils.loadAnimation(getContext(),R.anim.zoom_in_x_only);
        zoomInX.setDuration(400);
        ProgressBarAnimation progressBarAnimation = new ProgressBarAnimation(levelProgress,0,
                (playerDetails.getAccFrubies() * 100 )/nextLevel.getLevelFrubies());
        progressBarAnimation.setDuration(700);
        progressBarAnimation.setFillAfter(true);

        levelProgress.startAnimation(progressBarAnimation);
        frubiesAndPointsContainer.startAnimation(zoomInX);
        frubiesAndPointsContainer.startAnimation(fadeIn);
        achievemetTitle.startAnimation(fadeIn);
    }

    private void prepView() {
        achievementsRecyclerView.setHasFixedSize(true);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        achievementsRecyclerView.setAdapter(achievementsAdapter);
        achievementsRecyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    public void fillPlayerData(PlayerDetailsResponse playerDetails , Level nextLevel)
    {
        levelName.setText(playerDetails.getLevel().getName());
        if(playerDetails.getLevel().getIcon() != null)
            ImageDownloader.downloadImage(getContext(), levelLogo,
                    playerDetails.getLevel().getIcon().getFileName());
        frubiesForNextLevel.setText(nextLevel.getLevelFrubies() + "");
        currentPointsValue.setText(playerDetails.getAccPoints() + "");
        currentFrubiesValue.setText(playerDetails.getLevel().getLevelFrubies() + "");
        applyAnimation(playerDetails, nextLevel);

    }

    @Override
    public void fillAchievements(ArrayList<Game> games)
    {
        achievementsAdapter.setmData(games);
        achievementsAdapter.notifyDataSetChanged();
    }

    @Override
    public void showProfileLoadingIndicator()
    {
        profileLoadingIndicator.setVisibility(View.VISIBLE);
        profileLoadingIndicatorBg.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProfileLoadingIndicator()
    {
        profileLoadingIndicator.setVisibility(View.GONE);
        profileLoadingIndicatorBg.setVisibility(View.GONE);
    }

    @Override
    public void showAchievementsLoadingIndicator()
    {
        achievementsLoadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideAchievementsLoadingIndicator()
    {
        achievementsLoadingIndicator.setVisibility(View.GONE);
    }
}
