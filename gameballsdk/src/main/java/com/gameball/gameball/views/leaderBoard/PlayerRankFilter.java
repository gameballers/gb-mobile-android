package com.gameball.gameball.views.leaderBoard;

import com.gameball.gameball.model.response.PlayerInfo;

import io.reactivex.functions.Predicate;

public class PlayerRankFilter implements Predicate<PlayerInfo>
{
    private String playerId;

    public PlayerRankFilter(String playerId)
    {
        this.playerId = playerId;
    }


    @Override
    public boolean test(PlayerInfo playerInfo) throws Exception
    {
        return playerInfo.getExternalID().equals(playerId);
    }
}
