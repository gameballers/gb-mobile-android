package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetWithUnlocksWrapper
{
    @SerializedName("challenges")
    @Expose
    private ArrayList<Game> games;
    @SerializedName("quests")
    @Expose
    private ArrayList<Mission> missions;

    public ArrayList<Game> getGames()
    {
        return games;
    }

    public ArrayList<Mission> getMissions() {
        return missions;
    }
}
