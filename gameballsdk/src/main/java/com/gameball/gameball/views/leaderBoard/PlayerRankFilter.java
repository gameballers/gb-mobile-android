package com.gameball.gameball.views.leaderBoard;

import com.gameball.gameball.model.response.PlayerAttributes;

import io.reactivex.functions.Predicate;

public class PlayerRankFilter implements Predicate<PlayerAttributes> {
    private String playerUniqueId;

    public PlayerRankFilter(String playerUniqueId)
    {
        this.playerUniqueId = playerUniqueId;
    }


    @Override
    public boolean test(PlayerAttributes playerAttributes) throws Exception
    {
        return playerAttributes.getExternalID().equals(playerUniqueId);
    }
}
