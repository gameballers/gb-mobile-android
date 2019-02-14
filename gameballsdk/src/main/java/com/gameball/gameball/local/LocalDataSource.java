package com.gameball.gameball.local;

import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;

import java.util.ArrayList;

public class LocalDataSource
{
    public static LocalDataSource instance;

    public String clientId;
    public String playerId;
    public PlayerDetailsResponse playerDetails;
    public Level nextLevel;
    public ArrayList<Game> games;

    private LocalDataSource()
    {
        clientId = "8fdfd2dffd-9mnvhu25d6c3d";
        playerId = "SomeGuid16";
    }

    public static LocalDataSource getInstance()
    {
        if(instance == null)
            instance = new LocalDataSource();

        return instance;
    }
}
