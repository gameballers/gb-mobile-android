package com.gameball.gameball.views.profile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.R;

import java.util.ArrayList;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ItemRowHolder> {
    private Context mContext;
    private ArrayList<Object> mData;

    public AchievementsAdapter(Context context, ArrayList<Object> data) {
        this.mData = data;
        this.mContext = context;
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.acheivments_item_layout, parent, false);
        return new ItemRowHolder(row);
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position) {
//        Object item = mData.get(position);
        switch (position % 3) {
            case 1:
                holder.notAchievedIndicator.setVisibility(View.VISIBLE);
                break;
            case 2:
                holder.notAchievedIndicator.setVisibility(View.VISIBLE);
                holder.lockedAchievementIndicator.setVisibility(View.VISIBLE);
                break;
            default:
                holder.notAchievedIndicator.setVisibility(View.GONE);
                holder.lockedAchievementIndicator.setVisibility(View.GONE);
                holder.achievementProgress.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public int getItemCount() {
        return 6;
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView achievementsLogo;
        public TextView achievementName;
        public ProgressBar achievementProgress;
        public View notAchievedIndicator;
        public ImageView lockedAchievementIndicator;


        public ItemRowHolder(View itemView) {
            super(itemView);
            achievementsLogo = itemView.findViewById(R.id.achievements_logo);
            achievementName = itemView.findViewById(R.id.achievements_name);
            achievementProgress = itemView.findViewById(R.id.achievements_progress);
            notAchievedIndicator = itemView.findViewById(R.id.not_achieved_indicator);
            lockedAchievementIndicator = itemView.findViewById(R.id.locked_achievement_indicator);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1) {

            }
        }
    }
}
