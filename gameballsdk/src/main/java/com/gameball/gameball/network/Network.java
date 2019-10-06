package com.gameball.gameball.network;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.network.api.GameBallApi;
import com.gameball.gameball.network.interceptor.GeneralInterceptor;
import com.gameball.gameball.network.interceptor.HeaderInterceptor;
import com.gameball.gameball.network.interceptor.LoggingInterceptor;
import com.gameball.gameball.network.utils.RxErrorHandlingCallAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class Network {
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss a";
    private static final int TIMEOUT = 20;
    private static final Network ourInstance = new Network();
    private final GeneralInterceptor mGeneralInterceptor;
    private final OkHttpClient mOkHttpClient;
    private final Retrofit mRetrofit;
    private final Gson mGson;
    private final RxErrorHandlingCallAdapterFactory mRxCallAdapterFactory;
    private GameBallApi mGameBallApi;

    private Network() {
        mGeneralInterceptor = new GeneralInterceptor();

        OkHttpClient.Builder ohttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new LoggingInterceptor())
                .addInterceptor(new HeaderInterceptor())
                .addInterceptor(mGeneralInterceptor);

        mOkHttpClient = ohttpClientBuilder.build();


        mGson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

        mRxCallAdapterFactory = RxErrorHandlingCallAdapterFactory.create();

        String baseUrl;

        if (BuildConfig.DEBUG) {
            baseUrl = BuildConfig.STAGE_URL;
        } else {
            baseUrl = BuildConfig.LIVE_URL;
        }

        mRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(mOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create(mGson))
                .addCallAdapterFactory(mRxCallAdapterFactory)
                .build();
    }

    public static Network getInstance() {
        return ourInstance;
    }

    public GameBallApi getGameBallApi() {
        return mRetrofit.create(GameBallApi.class);
    }
}
