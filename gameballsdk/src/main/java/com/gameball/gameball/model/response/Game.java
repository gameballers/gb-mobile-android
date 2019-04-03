package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Game
{
    @SerializedName("GameName")
    @Expose
    private String gameName;
    @SerializedName("ChallengeId")
    @Expose
    private int challengeId;
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
    private int activationCriteriaTypeId;
    @SerializedName("ActivationFrubes")
    @Expose
    private int activationFrubes;
    @SerializedName("ActivationLevel")
    @Expose
    private int activationLevel;
    @SerializedName("LevelName")
    @Expose
    private String levelName;
    @SerializedName("BehaviorTypeId")
    @Expose
    private int behaviorTypeId;
    @SerializedName("TargetActionsCount")
    @Expose
    private int targetActionsCount;
    @SerializedName("TargetAmount")
    @Expose
    private int targetAmount;
    @SerializedName("ActionsCompletedPercentage")
    @Expose
    private double actionsCompletedPercentage;
    @SerializedName("AmountCompletedPercentage")
    @Expose
    private double amountCompletedPercentage;
    @SerializedName("ActionsAndAmountCompletedPercentage")
    @Expose
    private double actionsAndAmountCompletedPercentage;
    @SerializedName("IsRepeatable")
    @Expose
    private Boolean isRepeatable;
    @SerializedName("AchievedCount")
    @Expose
    private int achievedCount;
    @SerializedName("AchievedActionsCount")
    @Expose
    private int achievedActionsCount;
    @SerializedName("CurrentAmount")
    @Expose
    private int currentAmount;
    @SerializedName("UserMessage")
    @Expose
    private String userMessage;
    @SerializedName("Milestones")
    @Expose
    private ArrayList<MileStone> mileStones;

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(int challengeId) {
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

    public Boolean isUnlocked() {
        return isUnlocked;
    }

    public void setIsUnlocked(Boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
    }

    public int getActivationCriteriaTypeId() {
        return activationCriteriaTypeId;
    }

    public void setActivationCriteriaTypeId(int activationCriteriaTypeId) {
        this.activationCriteriaTypeId = activationCriteriaTypeId;
    }

    public int getActivationFrubes() {
        return activationFrubes;
    }

    public void setActivationFrubes(int activationFrubes) {
        this.activationFrubes = activationFrubes;
    }

    public int getActivationLevel() {
        return activationLevel;
    }

    public void setActivationLevel(int activationLevel) {
        this.activationLevel = activationLevel;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public int getBehaviorTypeId() {
        return behaviorTypeId;
    }

    public void setBehaviorTypeId(int behaviorTypeId) {
        this.behaviorTypeId = behaviorTypeId;
    }

    public int getTargetActionsCount() {
        return targetActionsCount;
    }

    public void setTargetActionsCount(int targetActionsCount) {
        this.targetActionsCount = targetActionsCount;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getActionsCompletedPercentage() {
        return actionsCompletedPercentage;
    }

    public void setActionsCompletedPercentage(double actionsCompletedPercentage) {
        this.actionsCompletedPercentage = actionsCompletedPercentage;
    }

    public double getAmountCompletedPercentage() {
        return amountCompletedPercentage;
    }

    public void setAmountCompletedPercentage(double amountCompletedPercentage) {
        this.amountCompletedPercentage = amountCompletedPercentage;
    }

    public double getActionsAndAmountCompletedPercentage() {
        return actionsAndAmountCompletedPercentage;
    }

    public void setActionsAndAmountCompletedPercentage(double actionsAndAmountCompletedPercentage) {
        this.actionsAndAmountCompletedPercentage = actionsAndAmountCompletedPercentage;
    }

    public Boolean isRepeatable() {
        return isRepeatable;
    }

    public void setIsRepeatable(Boolean isRepeatable) {
        this.isRepeatable = isRepeatable;
    }

    public int getAchievedCount() {
        return achievedCount;
    }

    public void setAchievedCount(int achievedCount) {
        this.achievedCount = achievedCount;
    }

    public int getAchievedActionsCount() {
        return achievedActionsCount;
    }

    public void setAchievedActionsCount(int achievedActionsCount) {
        this.achievedActionsCount = achievedActionsCount;
    }

    public int getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public ArrayList<MileStone> getMileStones()
    {
        return mileStones;
    }


}
