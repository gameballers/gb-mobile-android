package com.gameball.gameball.views.challengeDetails;

import android.content.Context;
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

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Milestone;
import com.gameball.gameball.utils.ProgressBarAnimation;

import java.util.ArrayList;

import static com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity.ACTION_AND_AMOUNT_BASED;
import static com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity.ACTION_BASED;
import static com.gameball.gameball.views.challengeDetails.ChallengeDetailsActivity.AMOUNT_BASED;

public class MilestonesAdapter extends RecyclerView.Adapter<MilestonesAdapter.ItemRowHolder>
{
    private Context mContext;
    private ArrayList<Milestone> mData;
    private ClientBotSettings clientBotSettings;
    private int behaviourTypeId;

    public MilestonesAdapter(Context context, ArrayList<Milestone> data, int behaviourTypeId)
    {
        this.mData = data;
        this.mContext = context;
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        this.behaviourTypeId = behaviourTypeId;
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.milestone_item_layout, parent, false);
        return new ItemRowHolder(row);
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position)
    {
        Milestone item = mData.get(position);
        holder.mileStoneRewardText.setText(String.format("%d %s | %d %s",item.getRewardFrubies(),
                mContext.getString(R.string.frubies), item.getRewardPoints(),
                mContext.getString(R.string.points)));

        holder.milestoneDescription.setText(item.getDescription());

        switch (behaviourTypeId)
        {
            case ACTION_BASED:
                showActionProgress(item, holder);
                break;
            case AMOUNT_BASED:
                showAmountProgress(item, holder);
                break;
            case ACTION_AND_AMOUNT_BASED:
                showAmountProgress(item, holder);
                showActionProgress(item, holder);
                break;

        }

        if(isMilestoneComplete(item))
            holder.milestoneIcon.setImageResource(R.drawable.ic_complete);

        setUpBotSettings(holder);
    }

    private boolean isMilestoneComplete(Milestone milestone)
    {
        boolean complete = true;

        if(milestone.getTargetAmount() != null && milestone.getAmountCompletedPercentage() < 100)
            complete = false;

        if(milestone.getTargetActionCount() != null && milestone.getActionsCompletedPercentage() < 100)
            complete = false;

        return complete;
    }

    private void setUpBotSettings(ItemRowHolder holder)
    {
        LayerDrawable amountProgress = (LayerDrawable) holder.milestoneAmountProgress.getProgressDrawable();
        amountProgress.setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()), PorterDuff.Mode.SRC_IN);

        LayerDrawable actionProgress = (LayerDrawable) holder.milestoneActionProgress.getProgressDrawable();
        actionProgress.setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()), PorterDuff.Mode.SRC_IN);
    }

    @Override
    public int getItemCount()
    {
        return mData.size();
    }

    public void setmData(ArrayList<Milestone> mData)
    {
        this.mData = mData;
    }

    private void showActionProgress(Milestone milestone, final ItemRowHolder holder)
    {
        holder.milestoneActionProgress.setVisibility(View.VISIBLE);
        holder.targetActionCount.setVisibility(View.VISIBLE);
        holder.targetActionCount.setText(milestone.getTargetActionCount() + "");
//        holder.milestoneDescription.setText(String.format("only %d %s remaining to achive this challenge",
//                milestone.getTargetActionCount() - milestone.getAchievedActionsCount(), ""));

        holder.milestoneActionProgress.setProgress(1);
        final ProgressBarAnimation actionProgressBarAnimation = new ProgressBarAnimation(holder.milestoneActionProgress,
                0,(int) milestone.getActionsCompletedPercentage());
        actionProgressBarAnimation.setDuration(700);
        actionProgressBarAnimation.setFillAfter(true);
        Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                holder.milestoneActionProgress.startAnimation(actionProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        holder.targetActionCount.startAnimation(fadeIn);
        holder.milestoneActionProgress.startAnimation(fadeIn);
        holder.milestoneDescription.startAnimation(fadeIn);
    }

    private void showAmountProgress(Milestone milestone, final ItemRowHolder holder)
    {
        holder.milestoneAmountProgress.setVisibility(View.VISIBLE);
        holder.targetAmountCount.setVisibility(View.VISIBLE);

        String targetAmountStr = "" + milestone.getTargetAmount();

        holder.targetAmountCount.setText(targetAmountStr);

//        holder.milestoneDescription.setText(String.format("only %d %s remaining to achive this challenge",
//                game.getTargetAmount() - game.getCurrentAmount(), ""));

        holder.milestoneAmountProgress.setProgress(1);
        if(milestone.getAmountCompletedPercentage() == 0)
            holder.milestoneAmountProgress.setProgress(0);
        final ProgressBarAnimation amountProgressBarAnimation = new ProgressBarAnimation(holder.milestoneAmountProgress,
                0,(int) milestone.getAmountCompletedPercentage());
        amountProgressBarAnimation.setDuration(700);
        amountProgressBarAnimation.setFillAfter(true);


        Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                holder.milestoneAmountProgress.startAnimation(amountProgressBarAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });

        holder.targetAmountCount.startAnimation(fadeIn);
        holder.milestoneAmountProgress.startAnimation(fadeIn);
        holder.milestoneDescription.startAnimation(fadeIn);
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
		public ImageView milestoneIcon;
        public TextView milestoneDescription;
        public TextView mileStoneRewardText;
        public ProgressBar milestoneActionProgress;
        public ProgressBar milestoneAmountProgress;
        public TextView targetAmountCount;
        public TextView targetActionCount;


        public ItemRowHolder(View itemView)
        {
            super(itemView);
            milestoneIcon = itemView.findViewById(R.id.milestone_icon);
            milestoneDescription = itemView.findViewById(R.id.milestone_description);
            mileStoneRewardText = itemView.findViewById(R.id.mileStone_reward_text);
            milestoneActionProgress = itemView.findViewById(R.id.milestone_action_progress);
            milestoneAmountProgress = itemView.findViewById(R.id.milestone_amount_progress);
            targetAmountCount = itemView.findViewById(R.id.milestone_target_amount_count);
            targetActionCount = itemView.findViewById(R.id.milestone_target_action_count);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1)
            {
                
            }
        }
    }
}
