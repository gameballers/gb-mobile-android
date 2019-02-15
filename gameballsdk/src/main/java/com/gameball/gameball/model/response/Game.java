package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Game
{
    @SerializedName("GameName")
    @Expose
    private String gameName;
    @SerializedName("ChallengeId")
    @Expose
    private Integer challengeId;
    @SerializedName("Icon")
    @Expose
    private String icon;
    @SerializedName("Description")
    @Expose
    private String description;
    @SerializedName("IsUnlocked")
    @Expose
    private Boolean isUnlocked;
    @SerializedName("ActivationCriteriaTypeId")
    @Expose
    private Integer activationCriteriaTypeId;
    @SerializedName("ActivationFrubes")
    @Expose
    private Integer activationFrubes;
    @SerializedName("ActivationLevel")
    @Expose
    private Integer activationLevel;
    @SerializedName("LevelName")
    @Expose
    private String levelName;
    @SerializedName("BehaviorTypeId")
    @Expose
    private Integer behaviorTypeId;
    @SerializedName("TargetActionsCount")
    @Expose
    private Integer targetActionsCount;
    @SerializedName("TargetAmount")
    @Expose
    private Integer targetAmount;
    @SerializedName("ActionsCompletedPercentage")
    @Expose
    private Double actionsCompletedPercentage;
    @SerializedName("AmountCompletedPercentage")
    @Expose
    private Integer amountCompletedPercentage;
    @SerializedName("ActionsAndAmountCompletedPercentage")
    @Expose
    private Double actionsAndAmountCompletedPercentage;
    @SerializedName("IsRepeatable")
    @Expose
    private Boolean isRepeatable;
    @SerializedName("AchievedCount")
    @Expose
    private Integer achievedCount;
    @SerializedName("AchievedActionsCount")
    @Expose
    private Integer achievedActionsCount;
    @SerializedName("CurrentAmount")
    @Expose
    private Integer currentAmount;
    @SerializedName("UserMessage")
    @Expose
    private String userMessage;

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Integer getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Integer challengeId) {
        this.challengeId = challengeId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsUnlocked() {
        return isUnlocked;
    }

    public void setIsUnlocked(Boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
    }

    public Integer getActivationCriteriaTypeId() {
        return activationCriteriaTypeId;
    }

    public void setActivationCriteriaTypeId(Integer activationCriteriaTypeId) {
        this.activationCriteriaTypeId = activationCriteriaTypeId;
    }

    public Integer getActivationFrubes() {
        return activationFrubes;
    }

    public void setActivationFrubes(Integer activationFrubes) {
        this.activationFrubes = activationFrubes;
    }

    public Integer getActivationLevel() {
        return activationLevel;
    }

    public void setActivationLevel(Integer activationLevel) {
        this.activationLevel = activationLevel;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public Integer getBehaviorTypeId() {
        return behaviorTypeId;
    }

    public void setBehaviorTypeId(Integer behaviorTypeId) {
        this.behaviorTypeId = behaviorTypeId;
    }

    public Integer getTargetActionsCount() {
        return targetActionsCount;
    }

    public void setTargetActionsCount(Integer targetActionsCount) {
        this.targetActionsCount = targetActionsCount;
    }

    public Integer getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(Integer targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Double getActionsCompletedPercentage() {
        return actionsCompletedPercentage;
    }

    public void setActionsCompletedPercentage(Double actionsCompletedPercentage) {
        this.actionsCompletedPercentage = actionsCompletedPercentage;
    }

    public Integer getAmountCompletedPercentage() {
        return amountCompletedPercentage;
    }

    public void setAmountCompletedPercentage(Integer amountCompletedPercentage) {
        this.amountCompletedPercentage = amountCompletedPercentage;
    }

    public double getActionsAndAmountCompletedPercentage() {
        return actionsAndAmountCompletedPercentage;
    }

    public void setActionsAndAmountCompletedPercentage(Double actionsAndAmountCompletedPercentage) {
        this.actionsAndAmountCompletedPercentage = actionsAndAmountCompletedPercentage;
    }

    public Boolean getIsRepeatable() {
        return isRepeatable;
    }

    public void setIsRepeatable(Boolean isRepeatable) {
        this.isRepeatable = isRepeatable;
    }

    public Integer getAchievedCount() {
        return achievedCount;
    }

    public void setAchievedCount(Integer achievedCount) {
        this.achievedCount = achievedCount;
    }

    public Integer getAchievedActionsCount() {
        return achievedActionsCount;
    }

    public void setAchievedActionsCount(Integer achievedActionsCount) {
        this.achievedActionsCount = achievedActionsCount;
    }

    public Integer getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(Integer currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
