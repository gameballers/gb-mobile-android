package com.gameball.gameball.network.interceptor;

import com.gameball.gameball.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 7/18/2017.
 */
public class GeneralInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String baseUrl;

        if (BuildConfig.DEBUG) {
            baseUrl = BuildConfig.STAGE_URL;
        } else {
            baseUrl = BuildConfig.LIVE_URL;
        }

        if (request.url().toString().startsWith(baseUrl)) {
            Request.Builder builder = request.newBuilder();
            // builder.addHeader(API.HEADER_X_API_KEY, LocalDataSourceImpl.ANDROID_API_KEY);
            return chain.proceed(builder.build());
        }
        return chain.proceed(request);
    }
}
