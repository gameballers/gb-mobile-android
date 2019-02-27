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
    private long playerCategoryID;
    @SerializedName("isPositive")
    @Expose
    private boolean isPositive;

    public Action(String challengeApiId)
    {
        this(challengeApiId,-1,0);
    }

    public Action(String challengeApiId, int amount)
    {
        this(challengeApiId,amount,0);
    }

    public Action(String challengeApiId, long playerCategoryID)
    {
        this(challengeApiId,-1,playerCategoryID);
    }

    public Action(String challengeApiId, int amount, long playerCategoryID)
    {
        this.challengeApiId = challengeApiId;
        this.playerId = SharedPreferencesUtils.getInstance().getPlayerId();
        this.playerCategoryID = playerCategoryID;
        this.isPositive = true;
        if(amount != -1)
            this.amount = amount;
    }
}
