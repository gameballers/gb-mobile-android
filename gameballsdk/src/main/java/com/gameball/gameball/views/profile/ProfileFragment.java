package com.gameball.gameball.views.profile;

import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;

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
    private RecyclerView achievementsRecyclerView;
    private ProgressBar profileLoadingIndicator;
    private ProgressBar achievementsLoadingIndicator;
    private View profileLoadingIndicatorBg;

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
        achievementsRecyclerView = rootView.findViewById(R.id.achievements_recyclerView);
        profileLoadingIndicator = rootView.findViewById(R.id.profile_data_loading_indicator);
        profileLoadingIndicatorBg = rootView.findViewById(R.id.profile_data_loading_indicator_bg);
        achievementsLoadingIndicator = rootView.findViewById(R.id.achievements_loading_indicator);
    }

    private void setupBotSettings()
    {
        LayerDrawable progressDrawable = (LayerDrawable) levelProgress.getProgressDrawable();
        ClipDrawable progressItem = (ClipDrawable) progressDrawable.findDrawableByLayerId(R.id.progress_item);
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
            ImageDownloader.downloadImage(levelLogo, Constants.TEST_BASE_URL +
                    playerDetails.getLevel().getIcon().getFileName());
        frubiesForNextLevel.setText(nextLevel.getLevelFrubies() + "");
        currentPointsValue.setText(playerDetails.getAccPoints() + "");
        currentFrubiesValue.setText(playerDetails.getLevel().getLevelFrubies() + "");
        Log.i("level_Progress",(playerDetails.getAccFrubies() /nextLevel.getLevelFrubies()) * 100 + "");
        levelProgress.setProgress((playerDetails.getAccFrubies() * 100 )/nextLevel.getLevelFrubies());
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
