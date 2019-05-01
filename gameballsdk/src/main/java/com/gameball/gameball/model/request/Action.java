package com.gameball.gameball.model.request;

import android.support.annotation.NonNull;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Action
{
    @SerializedName("ChallengeAPIID")
    @Expose
    private String challengeApiId;
    @SerializedName("QuestAPIID")
    @Expose
    private String questApiId;
    @SerializedName("ChallengeAPIIDs")
    @Expose
    private ArrayList<String> challengeApiIds;
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

    public Action(String challengeApiId, String questApiId)
    {
        this(challengeApiId,questApiId, null,-1);

        if(challengeApiId != null && questApiId != null)
            throw new IllegalArgumentException(
                    "Action: one and only one of challengeApiId and questApiId can have a value");

    }

    public Action(String challengeApiId, String questApiId, int amount)
    {
        this(challengeApiId,questApiId, null,amount);

        if(challengeApiId == null && questApiId == null)
        {
            throw new IllegalArgumentException(
                    "Action: Both challengeApiId and questApiId cannot be equal null");
        }

    }

    public Action(ArrayList<String> challengeApiIds)
    {
        this(null,null,challengeApiIds, -1);
    }

    public Action(ArrayList<String> challengeApiIds, int amount)
    {
        this(null,null,challengeApiIds, amount);
    }

    private Action(String challengeApiId,String questApiId, ArrayList<String> challengeApiIds,
                  int amount) throws IllegalArgumentException
    {
        if(challengeApiId != null)
        {
            this.challengeApiId = challengeApiId;
            this.questApiId = null;
            this.challengeApiIds = null;
        }
        else if(questApiId != null)
        {
            this.questApiId = questApiId;
            this.challengeApiId = null;
            this.challengeApiIds = null;
        }
        else if(challengeApiIds != null)
        {
            this.challengeApiIds = challengeApiIds;
            this.challengeApiId = null;
            this.questApiId = null;
        }
        else
        {
            throw new IllegalArgumentException("Action: At least one of the ID parameters must have a value");
        }
        this.playerId = SharedPreferencesUtils.getInstance().getPlayerId();
        this.isPositive = true;
        if(SharedPreferencesUtils.getInstance().getPlayerCategoryId() != -1)
        {
            this.playerCategoryID = SharedPreferencesUtils.getInstance().getPlayerCategoryId();
        }
        if(amount != -1)
        {
            this.amount = amount;
        }
    }
}
