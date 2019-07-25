package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Level {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("description")
    @Expose
    private Object description;
    @SerializedName("rewardFrubies")
    @Expose
    private Integer rewardFrubies;
    @SerializedName("rewardPoints")
    @Expose
    private Integer rewardPoints;
    @SerializedName("levelFrubies")
    @Expose
    private Integer levelFrubies;
    @SerializedName("levelOrder")
    @Expose
    private Integer levelOrder;
    @SerializedName("icon")
    @Expose
    private Icon icon;

    public Integer getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Object getDescription()
    {
        return description;
    }

    public Integer getRewardFrubies()
    {
        return rewardFrubies;
    }

    public Integer getRewardPoints()
    {
        return rewardPoints;
    }

    public Integer getLevelFrubies()
    {
        return levelFrubies;
    }

    public Integer getLevelOrder()
    {
        return levelOrder;
    }

    public Icon getIcon()
    {
        return icon;
    }
}