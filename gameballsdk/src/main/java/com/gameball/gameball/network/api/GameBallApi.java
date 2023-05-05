package com.gameball.gameball.network.api;

import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.model.request.Event;
import com.gameball.gameball.model.request.GenerateOTPBody;
import com.gameball.gameball.model.request.GetPlayerBalanceBody;
import com.gameball.gameball.model.request.HoldPointBody;
import com.gameball.gameball.model.request.PlayerInfoBody;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.request.RedeemPointBody;
import com.gameball.gameball.model.request.ReferralBody;
import com.gameball.gameball.model.request.ReverseHeldPointsbody;
import com.gameball.gameball.model.request.RewardPointBody;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.HoldPointsResponse;
import com.gameball.gameball.model.response.PlayerBalanceResponse;
import com.gameball.gameball.model.response.PlayerInfoResponse;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Config;

import io.reactivex.Completable;
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
    @POST(Config.InitializePlayer)
    Single<PlayerRegisterResponse> registrationPlayer(@Body PlayerRegisterRequest playerRegisterRequest);

    @GET(Config.GetBotSettings)
    Single<BaseResponse<ClientBotSettings>> getBotSettings();

    @POST(Config.AddEvent)
    Completable addEvent(@Body Event eventBody);
}
