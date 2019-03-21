package com.gameball.gameball.model.request;

import com.gameball.gameball.local.SharedPreferencesUtils;
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
    private Integer amount;
    @SerializedName("PlayerCategoryID")
    @Expose
    private Integer playerCategoryID;
    @SerializedName("IsPositive")
    @Expose
    private boolean isPositive;

    public Action(String challengeApiId)
    {
        this(challengeApiId,-1);
    }

    public Action(String challengeApiId, int amount)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = SharedPreferencesUtils.getInstance().getPlayerId();
        this.isPositive = true;
        this.playerCategoryID = SharedPreferencesUtils.getInstance().getPlayerCategoryId();
        if(amount != -1)
        {
            this.amount = amount;
        }
    }
}
