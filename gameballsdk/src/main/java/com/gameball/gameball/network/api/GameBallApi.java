package com.gameball.gameball.network.api;

import android.graphics.Bitmap;

import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.GetNextLevelWrapper;
import com.gameball.gameball.model.response.GetWithUnlocksWrapper;
import com.gameball.gameball.model.response.PlayerDetailsResponseWrapper;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Config;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public interface GameBallApi {
    @POST(Config.PlayerRegistration)
    Single<BaseResponse<PlayerRegisterResponse>> registrationPlayer(
            @Body PlayerRegisterRequest playerRegisterRequest);

    @POST(Config.Push)
    Single<Response<Void>> push(@Header("token") String token);

    @GET(Config.PlayerDetails)
    Observable<PlayerDetailsResponseWrapper> getPlayerDetails(@Query("externalId") String playerId);

    @GET(Config.GetWithUnlocks)
    Observable<GetWithUnlocksWrapper> getWithUnlocks(@Query("externalId") String playerId);

    @GET(Config.GetNextLevel)
    Observable<GetNextLevelWrapper> getNextLevel(@Query("externalId") String playerId);
}
