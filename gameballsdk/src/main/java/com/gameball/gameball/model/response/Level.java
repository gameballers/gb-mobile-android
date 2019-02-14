package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Level {

    @SerializedName("ClientID")
    @Expose
    private Integer clientID;
    @SerializedName("IsAchivedNotificationsOn")
    @Expose
    private Boolean isAchivedNotificationsOn;
    @SerializedName("IconID")
    @Expose
    private String iconID;
    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("Description")
    @Expose
    private Object description;
    @SerializedName("RewardFrubies")
    @Expose
    private Integer rewardFrubies;
    @SerializedName("RewardPoints")
    @Expose
    private Integer rewardPoints;
    @SerializedName("LevelFrubies")
    @Expose
    private Integer levelFrubies;
    @SerializedName("LevelOrder")
    @Expose
    private Integer levelOrder;
    @SerializedName("IconEnabled")
    @Expose
    private Boolean iconEnabled;
    @SerializedName("isDeleted")
    @Expose
    private Boolean isDeleted;
    @SerializedName("isDefault")
    @Expose
    private Boolean isDefault;
    @SerializedName("isActive")
    @Expose
    private Boolean isActive;
    @SerializedName("PlayerCategoryID")
    @Expose
    private Integer playerCategoryID;
    @SerializedName("Icon")
    @Expose
    private Icon icon;

    public Integer getClientID() {
        return clientID;
    }

    public void setClientID(Integer clientID) {
        this.clientID = clientID;
    }

    public Boolean getIsAchivedNotificationsOn() {
        return isAchivedNotificationsOn;
    }

    public void setIsAchivedNotificationsOn(Boolean isAchivedNotificationsOn) {
        this.isAchivedNotificationsOn = isAchivedNotificationsOn;
    }

    public String getIconID() {
        return iconID;
    }

    public void setIconID(String iconID) {
        this.iconID = iconID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

    public Integer getRewardFrubies() {
        return rewardFrubies;
    }

    public void setRewardFrubies(Integer rewardFrubies) {
        this.rewardFrubies = rewardFrubies;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public Integer getLevelFrubies() {
        return levelFrubies;
    }

    public void setLevelFrubies(Integer levelFrubies) {
        this.levelFrubies = levelFrubies;
    }

    public Integer getLevelOrder() {
        return levelOrder;
    }

    public void setLevelOrder(Integer levelOrder) {
        this.levelOrder = levelOrder;
    }

    public Boolean getIconEnabled() {
        return iconEnabled;
    }

    public void setIconEnabled(Boolean iconEnabled) {
        this.iconEnabled = iconEnabled;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPlayerCategoryID() {
        return playerCategoryID;
    }

    public void setPlayerCategoryID(Integer playerCategoryID) {
        this.playerCategoryID = playerCategoryID;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

}