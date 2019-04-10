package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Quest
{
    @SerializedName("QuestName")
    @Expose
    private String questName;
    @SerializedName("QuestId")
    @Expose
    private Integer questId;
    @SerializedName("Description")
    @Expose
    private Object description;
    @SerializedName("IsActive")
    @Expose
    private Boolean isActive;
    @SerializedName("IsReferral")
    @Expose
    private Boolean isReferral;
    @SerializedName("RewardFrubies")
    @Expose
    private Object rewardFrubies;
    @SerializedName("RewardPoints")
    @Expose
    private Object rewardPoints;
    @SerializedName("QuestChallenges")
    @Expose
    private ArrayList<Game> questChallenges = new ArrayList<>();

    public String getQuestName()
    {
        return questName;
    }

    public Integer getQuestId()
    {
        return questId;
    }

    public Object getDescription()
    {
        return description;
    }

    public Boolean getActive()
    {
        return isActive;
    }

    public Boolean getReferral()
    {
        return isReferral;
    }

    public Object getRewardFrubies()
    {
        return rewardFrubies;
    }

    public Object getRewardPoints()
    {
        return rewardPoints;
    }

    public ArrayList<Game> getQuestChallenges()
    {
        return questChallenges;
    }
}
