package com.gameball.gameball.views.profile;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.utils.ProgressBarAnimation;
import com.gameball.gameball.views.mainContainer.MainContainerContract;

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
    private TextView currentFrubiesTitle;
    private TextView currentPointTitle;
    private RecyclerView achievementsRecyclerView;
    private ProgressBar profileLoadingIndicator;
    private View profileLoadingIndicatorBg;


    AchievementsAdapter achievementsAdapter;
    ProfileContract.Presenter presenter;
    ClientBotSettings clientBotSettings;
    float playerProgress;

    MainContainerContract.View mainContainerView;

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
        presenter.getPlayerInfo(true);
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
        currentPointTitle = rootView.findViewById(R.id.points_title);
        currentFrubiesTitle= rootView.findViewById(R.id.frubies_title);

    }

    private void setupBotSettings()
    {
        LayerDrawable progressDrawable = (LayerDrawable) levelProgress.getProgressDrawable();
        progressDrawable.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()),
                PorterDuff.Mode.SRC_IN);
        achievemetTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        profileLoadingIndicator.getIndeterminateDrawable().setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()),
                PorterDuff.Mode.SRC_IN);
    }

    private void applyAnimation()
    {
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeIn.setDuration(700);
        Animation zoomInX = AnimationUtils.loadAnimation(getContext(),R.anim.zoom_in_x_only);
        zoomInX.setDuration(400);
        ProgressBarAnimation progressBarAnimation = new ProgressBarAnimation(levelProgress,0,
                playerProgress);
        progressBarAnimation.setDuration(700);
        progressBarAnimation.setFillAfter(true);

        levelProgress.startAnimation(progressBarAnimation);
        levelName.startAnimation(fadeIn);
        currentFrubiesValue.startAnimation(fadeIn);
        currentPointsValue.startAnimation(fadeIn);
        currentFrubiesTitle.startAnimation(fadeIn);
        currentPointTitle.startAnimation(fadeIn);
        achievemetTitle.startAnimation(fadeIn);
    }

    private void prepView() {
        achievementsRecyclerView.setHasFixedSize(true);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        achievementsRecyclerView.setAdapter(achievementsAdapter);
        achievementsRecyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    public void fillPlayerData(PlayerInfo playerInfo, Level nextLevel)
    {
        levelName.setText(playerInfo.getLevel().getName());
        if(playerInfo.getLevel().getIcon() != null)
            ImageDownloader.downloadImage(getContext(), levelLogo,
                    playerInfo.getLevel().getIcon().getFileName());
        frubiesForNextLevel.setText(nextLevel.getLevelFrubies() + "");
        currentPointsValue.setText(playerInfo.getAccPoints() + "");
        currentFrubiesValue.setText(playerInfo.getLevel().getLevelFrubies() + "");
        achievemetTitle.setVisibility(View.VISIBLE);
        playerProgress = (playerInfo.getAccFrubies() * 100 )/nextLevel.getLevelFrubies();
    }

    @Override
    public void fillAchievements(ArrayList<Game> games)
    {
        achievementsAdapter.setmData(games);
        achievementsAdapter.notifyDataSetChanged();
    }

    @Override
    public void showLoadingIndicator()
    {
        profileLoadingIndicator.setVisibility(View.VISIBLE);
        profileLoadingIndicatorBg.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingIndicator()
    {
        profileLoadingIndicator.setVisibility(View.GONE);
        profileLoadingIndicatorBg.setVisibility(View.GONE);
        applyAnimation();
    }
}
