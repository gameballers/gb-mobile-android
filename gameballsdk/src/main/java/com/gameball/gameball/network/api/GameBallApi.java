package com.gameball.gameball.network.api;

import com.gameball.gameball.model.request.Event;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Config;

import io.reactivex.Completable;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public interface GameBallApi {
    @POST(Config.InitializePlayer)
    Single<PlayerRegisterResponse> registrationPlayer(@Body PlayerRegisterRequest playerRegisterRequest);

    @GET(Config.GetBotSettings)
    Single<BaseResponse<ClientBotSettings>> getBotSettings();

    @POST(Config.SendEvent)
    Completable sendEvent(@Body Event eventBody);
}
