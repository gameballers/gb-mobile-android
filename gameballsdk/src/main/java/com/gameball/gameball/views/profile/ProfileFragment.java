package com.gameball.gameball.views.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Quest;
import com.gameball.gameball.views.mainContainer.MainContainerContract;

import java.util.ArrayList;

public class ProfileFragment extends Fragment  implements ProfileContract.View
{
    View rootView;
    private TextView achievementTitle;
    private RecyclerView achievementsRecyclerView;
    private ProgressBar profileLoadingIndicator;
    private View profileLoadingIndicatorBg;
    private RelativeLayout noInternetConnectionLayout;


    private ChallengesAdapter challengesAdapter;
    private ProfileContract.Presenter presenter;
    private ClientBotSettings clientBotSettings;
    private Animation fadeIn;
    private Animation zoomInX;

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
        presenter.getWithUnlocks();
        return rootView;
    }

    private void initComponents() {
        challengesAdapter = new ChallengesAdapter(getContext(), new ArrayList<Game>());
        presenter = new ProfilePresenter(getContext(), this);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();

        fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeIn.setDuration(500);
        zoomInX = AnimationUtils.loadAnimation(getContext(),R.anim.zoom_in_x_only);
        zoomInX.setDuration(400);
    }

    private void initView() {
        achievementTitle = rootView.findViewById(R.id.achievements_title);
        achievementsRecyclerView = rootView.findViewById(R.id.achievements_recyclerView);
        profileLoadingIndicator = rootView.findViewById(R.id.profile_data_loading_indicator);
        profileLoadingIndicatorBg = rootView.findViewById(R.id.profile_data_loading_indicator_bg);
        noInternetConnectionLayout = rootView.findViewById(R.id.no_internet_layout);

    }

    private void setupBotSettings()
    {
        profileLoadingIndicator.getIndeterminateDrawable().setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()),
                PorterDuff.Mode.SRC_IN);
        achievementTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
    }

    private void prepView() {
        achievementsRecyclerView.setHasFixedSize(true);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        achievementsRecyclerView.setAdapter(challengesAdapter);
    }

    private ArrayList<Game> buildQuestChallengesArray(ArrayList<Quest> quests)
    {
        ArrayList<Game> games = new ArrayList<>();

        if(quests != null)
            for (Quest quest: quests)
            {
                games.addAll(quest.getQuestChallenges());
            }

        return games;
    }

    @Override
    public void onWithUnlocksLoaded(ArrayList<Game> games)
    {
        challengesAdapter.setmData(games);
        challengesAdapter.notifyDataSetChanged();
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
        Animation fadeOut = AnimationUtils.loadAnimation(getContext(),
                android.R.anim.fade_out);
        fadeOut.setDuration(100);
        profileLoadingIndicatorBg.setAnimation(fadeOut);
    }

    @Override
    public void showNoInternetConnectionLayout() {
        noInternetConnectionLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
        {
            PowerManager pm = (PowerManager) (getContext()).getSystemService(Context.POWER_SERVICE);
            if((pm.isInteractive()))
            {
                presenter.unsubscribe();
            }
        }
        super.onStop();
    }
}
