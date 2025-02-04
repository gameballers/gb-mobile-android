package com.gameball.gameball.network.api;

import com.gameball.gameball.model.request.Event;
import com.gameball.gameball.model.request.CustomerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.CustomerRegisterResponse;
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
    @POST(Config.InitializeCustomer)
    Single<CustomerRegisterResponse> registerCustomer(@Body CustomerRegisterRequest customerRegisterRequest);

    @GET(Config.GetBotSettings)
    Single<BaseResponse<ClientBotSettings>> getBotSettings();

    @POST(Config.SendEvent)
    Completable sendEvent(@Body Event eventBody);
}
