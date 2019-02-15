package com.gameball.gameball.network.profileRemote;

import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Single;


public interface DataSourceContract
{
    Single<BaseResponse<PlayerDetailsResponse>> getPlayerDetails(String playerId);
    Single<BaseResponse<ArrayList<Game>>> getWithUnlocks(String playerId);
    Single<BaseResponse<Level>> getNextLevel(String playerId);
}
