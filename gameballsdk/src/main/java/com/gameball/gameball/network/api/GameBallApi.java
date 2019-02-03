package com.gameball.gameball.network.api;

import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Config;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public interface GameBallApi {
    @POST(Config.PlayerRegistration)
    Single<BaseResponse<PlayerRegisterResponse>> registrationPlayer(
            @Body PlayerRegisterRequest playerRegisterRequest);

    @POST(Config.Push)
    Single<Response<Void>> push(@Header("token") String token);
}
