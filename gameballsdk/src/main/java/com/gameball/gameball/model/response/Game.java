package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Game
{
    @SerializedName("gameName")
    @Expose
    private String gameName;
    @SerializedName("challengeId")
    @Expose
    private Integer challengeId;
    @SerializedName("icon")
    @Expose
    private String icon;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("isUnlocked")
    @Expose
    private Boolean isUnlocked;
    @SerializedName("activationCriteriaTypeId")
    @Expose
    private Integer activationCriteriaTypeId;
    @SerializedName("activationFrubes")
    @Expose
    private Integer activationFrubes;
    @SerializedName("activationLevel")
    @Expose
    private Integer activationLevel;
    @SerializedName("rewardFrubies")
    @Expose
    private Integer rewardFrubies;
    @SerializedName("rewardPoints")
    @Expose
    private Integer rewardPoints;
    @SerializedName("highScore")
    @Expose
    private Integer highScore;
    @SerializedName("highScoreAmount")
    @Expose
    private Integer highScoreAmount;
    @SerializedName("amountUnit")
    @Expose
    private String amountUnit;
    @SerializedName("levelName")
    @Expose
    private String levelName;
    @SerializedName("behaviorTypeId")
    @Expose
    private Integer behaviorTypeId;
    @SerializedName("behaviorType")
    @Expose
    private String behaviorType;
    @SerializedName("targetActionsCount")
    @Expose
    private Integer targetActionsCount;
    @SerializedName("targetAmount")
    @Expose
    private Integer targetAmount;
    @SerializedName("actionsCompletedPercentage")
    @Expose
    private Double actionsCompletedPercentage;
    @SerializedName("amountCompletedPercentage")
    @Expose
    private Double amountCompletedPercentage;
    @SerializedName("actionsAndAmountCompletedPercentage")
    @Expose
    private Double actionsAndAmountCompletedPercentage;
    @SerializedName("completionPercentage")
    @Expose
    private Double completionPercentage;
    @SerializedName("isRepeatable")
    @Expose
    private Boolean isRepeatable;
    @SerializedName("isReferral")
    @Expose
    private Boolean isReferral;
    @SerializedName("achievedCount")
    @Expose
    private Integer achievedCount;
    @SerializedName("achievedActionsCount")
    @Expose
    private Integer achievedActionsCount;
    @SerializedName("currentAmount")
    @Expose
    private Integer currentAmount;
    @SerializedName("userMessage")
    @Expose
    private String userMessage;
    @SerializedName("milestones")
    @Expose
    private ArrayList<Milestone> milestones;

    public String getGameName() {
        return gameName;
    }

    public Integer getChallengeId() {
        return challengeId;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Boolean isUnlocked() {
        return isUnlocked;
    }

    public Integer getActivationCriteriaTypeId() {
        return activationCriteriaTypeId;
    }

    public Integer getActivationFrubies() {
        return activationFrubes != null ? activationFrubes : 0;
    }

    public Integer getActivationLevel() {
        return activationLevel != null ? activationLevel : 0;
    }

    public String getLevelName() {
        return levelName;
    }

    public Integer getBehaviorTypeId() {
        return behaviorTypeId;
    }

    public Integer getTargetActionsCount() {
        return targetActionsCount != null ? targetActionsCount : 0;
    }

    public Integer getTargetAmount() {
        return targetAmount;
    }


    public Double getActionsCompletedPercentage() {
        return actionsCompletedPercentage;
    }


    public Double getAmountCompletedPercentage() {
        return amountCompletedPercentage;
    }


    public Double getActionsAndAmountCompletedPercentage() {
        return actionsAndAmountCompletedPercentage;
    }


    public Boolean isRepeatable() {
        return isRepeatable;
    }


    public Integer getAchievedCount() {
        return achievedCount;
    }


    public Integer getAchievedActionsCount() {
        return achievedActionsCount != null ? achievedActionsCount : 0;
    }

    public Integer getCurrentAmount() {
        return currentAmount;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public ArrayList<Milestone> getMilestones()
    {
        return milestones;
    }

    public Integer getRewardFrubies()
    {
        return rewardFrubies != null ? rewardFrubies : 0;
    }

    public Integer getRewardPoints()
    {
        return rewardPoints != null ? rewardPoints : 0;
    }

    public Integer getHighScore()
    {
        return highScore != null ? highScore : 0;
    }

    public Integer getHighScoreAmount()
    {
        return highScoreAmount;
    }

    public String getAmountUnit()
    {
        return amountUnit;
    }

    public Integer getActivationFrubes()
    {
        return activationFrubes;
    }

    public String getBehaviorType()
    {
        return behaviorType;
    }

    public Double getCompletionPercentage()
    {
        return completionPercentage != null ? completionPercentage : 0;
    }

    public Boolean isReferral()
    {
        return isReferral;
    }

    public boolean isAchieved()
    {
        return achievedCount > 0;
    }
}
