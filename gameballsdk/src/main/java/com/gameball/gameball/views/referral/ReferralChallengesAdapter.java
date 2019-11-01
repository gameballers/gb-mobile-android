package com.gameball.gameball.views.referral;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
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
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.ProgressBarAnimation;
import com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

public class ReferralChallengesAdapter extends RecyclerView.Adapter<ReferralChallengesAdapter.ItemViewHolder>
{
    private Context mContext;
    private ArrayList<Game> mData;
    ClientBotSettings clientBotSettings;

    public ReferralChallengesAdapter(Context context, ArrayList<Game> data)
    {
        this.mData = data;
        this.mContext = context;
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.referral_challenge_item_layout, parent, false);
        return new ItemViewHolder(row);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position)
    {
        Game item = mData.get(position);
        if(item.getGameName() != null)
            holder.challengeName.setText(item.getGameName());

        String rewardStr = String.format(Locale.getDefault(),"%d %s | %d %s",item.getRewardFrubies(),
                clientBotSettings.getRankPointsName(), item.getRewardPoints(),
                clientBotSettings.getWalletPointsName());

        holder.challengeRewardTxt.setText(rewardStr);

        Picasso.get()
                .load(item.getIcon())
                .into(holder.challengeIcon);


        //progressbar setup
        holder.challengeProgress.setProgress(0);

        LayerDrawable challengeProgress = (LayerDrawable) holder.challengeProgress.getProgressDrawable();
        challengeProgress.setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);

        if (item.getCompletionPercentage() != null) {
            final ProgressBarAnimation progressAnimation = new ProgressBarAnimation(holder.challengeProgress,
                    0, item.getCompletionPercentage().intValue());
            progressAnimation.setDuration(700);
            progressAnimation.setFillAfter(true);
            holder.challengeProgress.startAnimation(progressAnimation);
        }

        if(item.getAchievedCount() >= 1)
            holder.challengeCheck.setImageResource(R.drawable.ic_checkmark);
        else
            holder.challengeCheck.setImageTintList(ColorStateList.
                    valueOf(Color.parseColor("#f2f2f2")));
    }

    @Override
    public int getItemCount()
    {
        return mData.size();
    }

    public void setmData(ArrayList<Game> mData)
    {
        this.mData = mData;
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
		public ImageView challengeIcon;
        public ImageView challengeCheck;
        public TextView challengeAchievedCount;
        public TextView challengeName;
        public TextView challengeRewardTxt;
        public ProgressBar challengeProgress;


        public ItemViewHolder(View itemView)
        {
            super(itemView);
            challengeIcon = itemView.findViewById(R.id.challenge_icon);
            challengeCheck = itemView.findViewById(R.id.challenge_check);
            challengeAchievedCount = itemView.findViewById(R.id.challenge_achieved_count);
            challengeName = itemView.findViewById(R.id.challenge_name);
            challengeRewardTxt = itemView.findViewById(R.id.challenge_reward_txt);
            challengeProgress = itemView.findViewById(R.id.challenge_event_progress);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1)
            {
                Intent intent = new Intent(mContext, ChallengeDetailsActivity.class);
                intent.putExtra(Constants.GAME_OBJ_KEY,new Gson().toJson(mData.get(pos)));
                mContext.startActivity(intent);
            }
        }
    }
}
