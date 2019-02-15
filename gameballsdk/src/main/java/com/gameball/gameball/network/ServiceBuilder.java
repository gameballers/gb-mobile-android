package com.gameball.gameball.network;

import android.support.annotation.NonNull;

import com.gameball.gameball.network.interceptor.HeaderInterceptor;
import com.gameball.gameball.network.interceptor.LoggingInterceptor;
import com.gameball.gameball.utils.Constants;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceBuilder
{
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .connectTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(new HeaderInterceptor())
            .addInterceptor(new LoggingInterceptor());

    private static Retrofit.Builder retrofitBuilder;

    private static void initRetrofitBuilder(@NonNull Gson gson)
    {
        retrofitBuilder = new Retrofit.Builder()
                .baseUrl(Constants.TEST_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    public static <S> S buildService(Class<S> serviceClass, @NonNull Gson gson)
    {
        initRetrofitBuilder(gson);
        Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
        return retrofit.create(serviceClass);
    }
}
