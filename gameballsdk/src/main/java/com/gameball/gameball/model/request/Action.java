package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Action
{
    @SerializedName("ChallengeAPIID")
    @Expose
    private String challengeApiId;
    @SerializedName("PlayerUniqueID")
    @Expose
    private String playerId;
    @SerializedName("Amount")
    @Expose
    private int amount;
    @SerializedName("PlayerCategoryID")
    @Expose
    private long playerCategoryID;
    @SerializedName("isPositive")
    @Expose
    private boolean isPositive;

    public Action(String challengeApiId, String playerId)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = playerId;
        playerCategoryID = 0;
        isPositive = true;
    }

    public Action(String challengeApiId, String playerId, int amount)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = playerId;
        this.amount = amount;
        this.isPositive = true;
    }

    public Action(String challengeApiId, String playerId, long playerCategoryID)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = playerId;
        this.playerCategoryID = playerCategoryID;
        this.isPositive = true;
    }

    public Action(String challengeApiId, String playerId, int amount, long playerCategoryID)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = playerId;
        this.amount = amount;
        this.playerCategoryID = playerCategoryID;
        this.isPositive = true;
    }
}
