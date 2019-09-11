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
    private int challengeId;
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
    private int activationCriteriaTypeId;
    @SerializedName("activationFrubes")
    @Expose
    private int activationFrubes;
    @SerializedName("activationLevel")
    @Expose
    private int activationLevel;
    @SerializedName("rewardFrubies")
    @Expose
    private int rewardFrubies;
    @SerializedName("rewardPoints")
    @Expose
    private int rewardPoints;
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
    private int behaviorTypeId;
    @SerializedName("behaviorType")
    @Expose
    private String behaviorType;
    @SerializedName("targetActionsCount")
    @Expose
    private int targetActionsCount;
    @SerializedName("targetAmount")
    @Expose
    private int targetAmount;
    @SerializedName("actionsCompletedPercentage")
    @Expose
    private double actionsCompletedPercentage;
    @SerializedName("amountCompletedPercentage")
    @Expose
    private double amountCompletedPercentage;
    @SerializedName("actionsAndAmountCompletedPercentage")
    @Expose
    private double actionsAndAmountCompletedPercentage;
    @SerializedName("completionPercentage")
    @Expose
    private double completionPercentage;
    @SerializedName("isRepeatable")
    @Expose
    private Boolean isRepeatable;
    @SerializedName("isReferral")
    @Expose
    private Boolean isReferral;
    @SerializedName("achievedCount")
    @Expose
    private int achievedCount;
    @SerializedName("achievedActionsCount")
    @Expose
    private int achievedActionsCount;
    @SerializedName("currentAmount")
    @Expose
    private int currentAmount;
    @SerializedName("userMessage")
    @Expose
    private String userMessage;
    @SerializedName("milestones")
    @Expose
    private ArrayList<Milestone> milestones;

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

    public int getActivationFrubies() {
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

    public ArrayList<Milestone> getMilestones()
    {
        return milestones;
    }

    public int getRewardFrubies()
    {
        return rewardFrubies;
    }

    public int getRewardPoints()
    {
        return rewardPoints;
    }

    public Integer getHighScore()
    {
        return highScore;
    }

    public Integer getHighScoreAmount()
    {
        return highScoreAmount;
    }

    public String getAmountUnit()
    {
        return amountUnit;
    }

    public int getActivationFrubes()
    {
        return activationFrubes;
    }

    public String getBehaviorType()
    {
        return behaviorType;
    }

    public double getCompletionPercentage()
    {
        return completionPercentage;
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
