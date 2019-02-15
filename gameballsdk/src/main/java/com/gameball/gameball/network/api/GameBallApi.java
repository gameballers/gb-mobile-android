package com.gameball.gameball.network.api;

import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerDetailsResponse;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Config;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

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
    Single<BaseResponse<PlayerDetailsResponse>> getPlayerDetails(@Query("externalId") String playerId);

    @GET(Config.GetWithUnlocks)
    Single<BaseResponse<ArrayList<Game>>> getWithUnlocks(@Query("externalId") String playerId);

    @GET(Config.GetNextLevel)
    Single<BaseResponse<Level>> getNextLevel(@Query("externalId") String playerId);
}
