package com.gameball.gameball.views.profile;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Locale;

import static com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity.HIGH_SCORE_BASED;

public class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.ItemRowHolder> {
    private Context mContext;
    private ArrayList<Game> mData;
    private ClientBotSettings clientBotSettings;
    private Animation translate;
    private Animation fadeIn;

    public ChallengesAdapter(Context context, ArrayList<Game> data) {
        this.mData = data;
        this.mContext = context;
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        translate = AnimationUtils.loadAnimation(mContext, R.anim.translate_bottom_to_top);
        translate.setDuration(800);
        fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        fadeIn.setDuration(800);
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.acheivments_item_layout, parent, false);
        ItemRowHolder rh = new ItemRowHolder(row);
        rh.setIsRecyclable(false);
        return rh;
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position) {
        Game item = mData.get(position);

        if(item.getBehaviorTypeId() == HIGH_SCORE_BASED)
        {
            if(item.getHighScore() != null && item.getHighScore() > 0)
                holder.challengeRewardPts.setText(String.format(Locale.getDefault(),
                        "%d %s", item.getHighScore(),
                    item.getAmountUnit()));
            else
                holder.challengeRewardPts.setVisibility(View.GONE);
        }
        else if(item.getRewardPoints() > 0)
            holder.challengeRewardPts.setText(String.format(Locale.getDefault(),
                    "%d %s", item.getRewardPoints(),
                mContext.getString(R.string.pts)));
        else
            holder.challengeRewardPts.setVisibility(View.GONE);
        
        if(item.getIcon() != null && !item.getIcon().isEmpty())
            ImageDownloader.downloadImage(mContext, holder.achievementsLogo, item.getIcon());

        holder.achievementName.setText(String.format(Locale.getDefault(),item.getGameName()));

        if(item.isUnlocked())
        {
            if(item.isAchieved())
            {
                    holder.notAchievedIndicator.setVisibility(View.GONE);
                    if(item.getAchievedCount() > 1)
                    {
                        holder.achievedCount.setText(String.format(Locale.getDefault(),
                                "%d", item.getAchievedCount()));
                        holder.achievedCount.setBackgroundTintList(ColorStateList.valueOf(
                                Color.parseColor(clientBotSettings.getBotMainColor())));
                        holder.achievedCount.setVisibility(View.VISIBLE);
                    }
            } else if (item.getCompletionPercentage() == 100)
            {
                holder.notAchievedIndicator.setVisibility(View.GONE);
            }

            holder.lockedAchievementIndicator.setVisibility(View.GONE);
        }

//        holder.itemview.startAnimation(fadeIn);
//        holder.itemview.startAnimation(translate);
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
        public View itemview;
        public ImageView achievementsLogo;
        public TextView achievementName;
        public View notAchievedIndicator;
        public ImageView lockedAchievementIndicator;
        public TextView challengeRewardPts;
        public TextView achievedCount;


        public ItemRowHolder(View itemView) {
            super(itemView);
            this.itemview = itemView;
            achievementsLogo = itemView.findViewById(R.id.challenge_icon);
            achievementName = itemView.findViewById(R.id.challenge_name);
            challengeRewardPts = itemView.findViewById(R.id.challenge_reward_points);
            notAchievedIndicator = itemView.findViewById(R.id.not_achieved_indicator);
            lockedAchievementIndicator = itemView.findViewById(R.id.locked_achievement_indicator);
            achievedCount = itemView.findViewById(R.id.achieved_count);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1) {
                Intent intent = new Intent(mContext, ChallengeDetailsActivity.class);
                intent.putExtra(Constants.GAME_OBJ_KEY,new Gson().toJson(mData.get(pos)));
                mContext.startActivity(intent);
            }
        }
    }
}
