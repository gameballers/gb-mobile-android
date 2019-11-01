package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Mission {
    @SerializedName("questName")
    @Expose
    private String questName;
    @SerializedName("questId")
    @Expose
    private Integer questId;
    @SerializedName("icon")
    @Expose
    private String icon;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("isActive")
    @Expose
    private Boolean isActive;
    @SerializedName("isReferral")
    @Expose
    private Boolean isReferral;
    @SerializedName("isOrdered")
    @Expose
    private Boolean isOrdered;
    @SerializedName("rewardFrubies")
    @Expose
    private Integer rewardFrubies;
    @SerializedName("rewardPoints")
    @Expose
    private Integer rewardPoints;
    @SerializedName("completionPercentage")
    @Expose
    private Double completionPercentage;
    @SerializedName("questChallenges")
    @Expose
    private ArrayList<Game> questChallenges = new ArrayList<>();

    public String getQuestName() {
        return questName;
    }

    public Integer getQuestId() {
        return questId;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return isActive;
    }

    public Boolean getReferral() {
        return isReferral;
    }

    public Boolean isOrdered() {
        return isOrdered;
    }

    public Integer getRewardFrubies() {
        return rewardFrubies;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage.intValue();
    }

    public ArrayList<Game> getQuestChallenges() {
        return questChallenges;
    }
}
