package com.gameball.gameball.views.leaderBoard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gameball.gameball.R;

import java.util.ArrayList;

public class LeaderBoardFragment extends Fragment {
    View rootView;
    private TextView filerBtn;
    private RecyclerView leaderboardRecyclerview;

    LeaderBoardAdapter leaderBoardAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        leaderBoardAdapter = new LeaderBoardAdapter(getContext(), new ArrayList<Object>());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_leader_board, container, false);
        initView();
        prepView();
        return rootView;
    }

    private void initView() {
        filerBtn = rootView.findViewById(R.id.filer_btn);
        leaderboardRecyclerview = rootView.findViewById(R.id.leaderboard_recyclerview);
    }

    private void prepView() {
        leaderboardRecyclerview.setHasFixedSize(true);
        leaderboardRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerview.setAdapter(leaderBoardAdapter);
    }
}
