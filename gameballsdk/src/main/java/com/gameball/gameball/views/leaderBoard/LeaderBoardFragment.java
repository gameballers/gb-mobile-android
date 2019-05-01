package com.gameball.gameball.views.leaderBoard;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.PlayerInfo;

import java.util.ArrayList;

public class LeaderBoardFragment extends Fragment implements LeaderBoardContract.View
{
    View rootView;
    private TextView filerBtn;
    private TextView leaderTitle;
    private RecyclerView leaderboardRecyclerview;
    private ProgressBar loadingIndicator;

    LeaderBoardAdapter leaderBoardAdapter;
    LeaderBoardContract.Presenter presenter;
    ClientBotSettings clientBotSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new LeaderBoardPresenter(getContext(),this);
        leaderBoardAdapter = new LeaderBoardAdapter(getContext(), new ArrayList<PlayerInfo>());
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_leader_board, container, false);
        initView();
        setupBotSettings();
        prepView();
        presenter.getLeaderBoard();
        return rootView;
    }

    private void initView() {
        filerBtn = rootView.findViewById(R.id.filer_btn);
        leaderboardRecyclerview = rootView.findViewById(R.id.leaderboard_recyclerview);
        loadingIndicator = rootView.findViewById(R.id.loading_indicator);
        leaderTitle = rootView.findViewById(R.id.leaderboard_title);
    }

    private void setupBotSettings()
    {
        leaderTitle.setTextColor(Color.parseColor(clientBotSettings.getButtonBackgroundColor()));
    }

    private void prepView() {
        leaderboardRecyclerview.setHasFixedSize(true);
        leaderboardRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerview.setAdapter(leaderBoardAdapter);
    }

    @Override
    public void fillLeaderBoard(ArrayList<PlayerInfo> leaderBoard)
    {
        leaderBoardAdapter.setmData(leaderBoard);
        leaderBoardAdapter.notifyDataSetChanged();
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
