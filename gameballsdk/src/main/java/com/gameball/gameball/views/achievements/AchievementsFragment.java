package com.gameball.gameball.views.achievements;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.views.profile.AchievementsAdapter;

import java.util.ArrayList;

public class AchievementsFragment extends Fragment implements AchievemetsContract.View
{
    View rootView;
    private RecyclerView achievementsRecyclerView;
    private ProgressBar loadingIndicator;
    private TextView achievementTitle;

    AchievementsAdapter achievementsAdapter;
    AchievemetsContract.Presenter presenter;
    ClientBotSettings clientBotSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        achievementsAdapter = new AchievementsAdapter(getContext(), new ArrayList<Game>());
        presenter = new AchievementsPresenter(getContext(), this);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_achievements, container, false);
        initView();
        setupBotSettings();
        presenter.getAchievements();
        return rootView;
    }

    private void initView() {
        achievementTitle = rootView.findViewById(R.id.achievements_title);
        achievementsRecyclerView = rootView.findViewById(R.id.achievements_recyclerView);
        loadingIndicator = rootView.findViewById(R.id.loading_indicator);
        achievementsRecyclerView.setHasFixedSize(true);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        achievementsRecyclerView.setAdapter(achievementsAdapter);
    }

    private void setupBotSettings()
    {
        achievementTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
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
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingIndicator()
    {
        loadingIndicator.setVisibility(View.GONE);
    }
}
