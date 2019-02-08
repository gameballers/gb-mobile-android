package com.gameball.gameball.views.achievements;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gameball.gameball.R;
import com.gameball.gameball.views.profile.AchievementsAdapter;

import java.util.ArrayList;

public class AchievementsFragment extends Fragment
{
    View rootView;
    private RecyclerView achievementsRecyclerView;

    AchievementsAdapter achievementsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        achievementsAdapter = new AchievementsAdapter(getContext(), new ArrayList<Object>());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_achievements, container, false);
        initView();
        return rootView;
    }

    private void initView()
    {
        achievementsRecyclerView = (RecyclerView) rootView.findViewById(R.id.achievements_recyclerView);
        achievementsRecyclerView.setHasFixedSize(true);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),3));
        achievementsRecyclerView.setAdapter(achievementsAdapter);

    }
}
