package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MileStone
{
    @SerializedName("MileStoneName")
    @Expose
    private String milestoneName;
    @SerializedName("MileStoneId")
    @Expose
    private int milestoneId;
    @SerializedName("Description")
    @Expose
    private String description;
    @SerializedName("RewardFrubies")
    @Expose
    private Integer rewardFrubies;
    @SerializedName("RewardPoints")
    @Expose
    private Integer rewardPoints;
    @SerializedName("TargetAmount")
    @Expose
    private Integer targetAmount;
    @SerializedName("TargetActionsCount")
    @Expose
    private Integer targetActionCount;
    @SerializedName("AchievedCount")
    @Expose
    private int AchievedCount;
    @SerializedName("ActionsCompletedPercentage")
    @Expose
    private double actionsCompletedPercentage;
    @SerializedName("AmountCompletedPercentage")
    @Expose
    private double amountCompletedPercentage;
    @SerializedName("ActionsAndAmountCompletedPercentage")
    @Expose
    private double actionsAndAmountCompletedPercentage;

    public String getMilestoneName()
    {
        return milestoneName;
    }

    public int getMilestoneId()
    {
        return milestoneId;
    }

    public String getDescription()
    {
        return description;
    }

    public Integer getRewardFrubies()
    {
        return rewardFrubies;
    }

    public Integer getRewardPoints()
    {
        return rewardPoints;
    }

    public Integer getTargetAmount()
    {
        return targetAmount;
    }

    public Integer getTargetActionCount()
    {
        return targetActionCount;
    }

    public int getAchievedCount()
    {
        return AchievedCount;
    }

    public double getActionsCompletedPercentage()
    {
        return actionsCompletedPercentage;
    }

    public double getAmountCompletedPercentage()
    {
        return amountCompletedPercentage;
    }

    public double getActionsAndAmountCompletedPercentage()
    {
        return actionsAndAmountCompletedPercentage;
    }
}
