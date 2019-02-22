package com.gameball.gameball.views.profile;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.network.utils.DownloadImage;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.views.achievementDetails.AchievementDetailsActivity;
import com.google.gson.Gson;

import java.util.ArrayList;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ItemRowHolder> {
    private Context mContext;
    private ArrayList<Game> mData;

    public AchievementsAdapter(Context context, ArrayList<Game> data) {
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
        Game item = mData.get(position);
        if(item.getIcon() != null && !item.getIcon().isEmpty())
            ImageDownloader.downloadImage(holder.achievementsLogo, BuildConfig.MAIN_HOST +
                    item.getIcon());

        holder.achievementName.setText(item.getGameName());
        if(item.getIsUnlocked())
        {
            holder.achievementProgress.setProgress((int) item.getActionsAndAmountCompletedPercentage());
            holder.achievementProgress.setVisibility(View.VISIBLE);

            if(item.getActionsAndAmountCompletedPercentage() == 100)
            {
                holder.notAchievedIndicator.setVisibility(View.GONE);
            }
            holder.lockedAchievementIndicator.setVisibility(View.GONE);
        }

    }

    public void setmData(ArrayList<Game> mData)
    {
        this.mData = mData;
    }

    @Override
    public int getItemCount() {
        return mData.size();
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
                Intent intent = new Intent(mContext, AchievementDetailsActivity.class);
                intent.putExtra(Constants.GAME_OBJ_KEY,new Gson().toJson(mData.get(pos)));
                mContext.startActivity(intent);
            }
        }
    }
}