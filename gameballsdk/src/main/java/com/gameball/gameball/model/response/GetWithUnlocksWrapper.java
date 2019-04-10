package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetWithUnlocksWrapper
{

    @SerializedName("Quests")
    @Expose
    private ArrayList<Quest> quests;
    @SerializedName("Challenges")
    @Expose
    private ArrayList<Game> games;

    public ArrayList<Game> getGames()
    {
        return games;
    }

    public void setGames(ArrayList<Game> games)
    {
        this.games = games;
    }

    public ArrayList<Quest> getQuests()
    {
        return quests;
    }
}
