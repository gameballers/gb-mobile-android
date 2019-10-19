package com.gameball.gameball.views.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
import com.gameball.gameball.utils.ImageDownloader;

import java.util.ArrayList;

public class MissionChallengesAdapter extends RecyclerView.Adapter<MissionChallengesAdapter.ItemRowHolder> {

    private Context context;
    private ArrayList<Game> mData;
    private boolean isOrdered;
    private ClientBotSettings clientBotSettings;

    public MissionChallengesAdapter(Context context, ArrayList<Game> mData, boolean isOrdered) {
        this.context = context;
        this.mData = mData;
        this.isOrdered = isOrdered;
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    @NonNull
    @Override
    public ItemRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.mission_challenges_item_layout, parent, false);
        ItemRowHolder rh = new ItemRowHolder(row);
        return rh;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemRowHolder holder, int position) {
        Game item = mData.get(position);

        ImageDownloader.downloadImage(context, holder.icon, item.getIcon());
        holder.name.setText(item.getGameName());
        if (isOrdered && position < mData.size() - 1) {
            holder.arrowIcon.setVisibility(View.VISIBLE);
        } else {
            holder.arrowIcon.setVisibility(View.GONE);
        }

        if (!item.isUnlocked()) {
            holder.notAchievedIndicator.setVisibility(View.VISIBLE);
            holder.greenCheckIcon.setVisibility(View.GONE);
            holder.challengeProgress.setVisibility(View.VISIBLE);
            holder.challengeAchievedCount.setVisibility(View.GONE);
            holder.lockedIndicator.setVisibility(View.VISIBLE);
            holder.challengeProgress.setProgress(0);
        } else {
            if (item.isAchieved()) {
                holder.notAchievedIndicator.setVisibility(View.GONE);
                holder.greenCheckIcon.setVisibility(View.VISIBLE);
                holder.challengeProgress.setVisibility(View.GONE);
                holder.challengeAchievedCount.setVisibility(View.VISIBLE);
                holder.lockedIndicator.setVisibility(View.GONE);

                holder.challengeAchievedCount.setText(
                        context.getString(R.string.mission_challenge_achieved_count,
                                item.getAchievedCount())
                );
            } else {
                holder.notAchievedIndicator.setVisibility(View.VISIBLE);
                holder.greenCheckIcon.setVisibility(View.GONE);
                holder.challengeProgress.setVisibility(View.VISIBLE);
                holder.challengeAchievedCount.setVisibility(View.GONE);
                holder.lockedIndicator.setVisibility(View.GONE);

                holder.challengeProgress.setProgress((int) item.getCompletionPercentage());
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setmData(ArrayList<Game> mData) {
        this.mData = mData;
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private ImageView notAchievedIndicator;
        private ImageView lockedIndicator;
        private TextView name;
        private ImageView greenCheckIcon;
        private ProgressBar challengeProgress;
        private TextView challengeAchievedCount;
        private ImageView arrowIcon;


        public ItemRowHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.challenge_icon);
            notAchievedIndicator = itemView.findViewById(R.id.not_achieved_indicator);
            lockedIndicator = itemView.findViewById(R.id.locked_challenge_indicator);
            name = itemView.findViewById(R.id.challenge_name);
            greenCheckIcon = itemView.findViewById(R.id.green_check_icon);
            challengeProgress = itemView.findViewById(R.id.challenge_progress);
            challengeAchievedCount = itemView.findViewById(R.id.challenge_achieved_count);
            arrowIcon = itemView.findViewById(R.id.arrow_icon);
            arrowIcon.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()));

            LayerDrawable eventProgress = (LayerDrawable) challengeProgress.getProgressDrawable();
            eventProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
        }
    }
}
