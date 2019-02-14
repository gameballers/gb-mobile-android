package com.gameball.gameball.network.profileRemote;

import android.graphics.Bitmap;

import com.gameball.gameball.model.response.GetNextLevelWrapper;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.PlayerDetailsResponseWrapper;
import com.gameball.gameball.network.GenericCallback;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;


public interface DataSourceContract
{
    Observable<PlayerDetailsResponseWrapper> getPlayerDetails(String playerId);
    Observable<GetWithUnlocksWrapper> getWithUnlocks(String playerId);
    Observable<GetNextLevelWrapper> getNextLevel(String playerId);
}
