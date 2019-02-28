package com.gameball.gameball.views.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
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
    private ClientBotSettings clientBotSettings;
    Animation bounceAnim;
    Animation fadeIn;

    public AchievementsAdapter(Context context, ArrayList<Game> data) {
        this.mData = data;
        this.mContext = context;
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        bounceAnim = AnimationUtils.loadAnimation(mContext, R.anim.translate_bottom_to_top);
        bounceAnim.setDuration(800);
        fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        fadeIn.setDuration(400);
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
        if(item.getIcon() != null && !item.getIcon().isEmpty())
            ImageDownloader.downloadImage(mContext, holder.achievementsLogo, item.getIcon());

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

        LayerDrawable progressDrawable = (LayerDrawable) holder.achievementProgress.getProgressDrawable();
        progressDrawable.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()),
                PorterDuff.Mode.SRC_IN);
        holder.itemview.startAnimation(fadeIn);
        holder.itemview.startAnimation(bounceAnim);
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
        public ProgressBar achievementProgress;
        public View notAchievedIndicator;
        public ImageView lockedAchievementIndicator;


        public ItemRowHolder(View itemView) {
            super(itemView);
            this.itemview = itemView;
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
