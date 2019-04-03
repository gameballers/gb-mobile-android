package com.gameball.gameball.network.profileRemote;

import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.model.response.PlayerInfoResponse;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Single;


public interface DataSourceContract
{
    Single<BaseResponse<PlayerInfoResponse>> getPlayerInfo(String playerId);
    Single<BaseResponse<GetWithUnlocksWrapper>> getWithUnlocks(String playerId);
    Single<BaseResponse<ArrayList<PlayerInfo>>> getLeaderBoard(String playerId);
    Single<BaseResponse<ClientBotSettings>> getBotSettings();
    Completable AddNewAction(Action actionBody);
}
