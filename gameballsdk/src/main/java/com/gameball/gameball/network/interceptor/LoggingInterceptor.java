package com.gameball.gameball.network.interceptor;

import static com.gameball.gameball.utils.Constants.TAG;

import android.util.Log;

import com.gameball.gameball.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoggingInterceptor implements Interceptor
{
    @Override
    public Response intercept(Chain chain) throws IOException
    {
        Request request = chain.request();
        long t1 = System.nanoTime();

        if (BuildConfig.DEBUG)
        {
            Log.d(TAG, request.toString());
            Log.d(TAG, String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));
        }

        Response response = chain.proceed(request);
        if (BuildConfig.DEBUG)
        {
            Log.d(TAG, response.toString());
            Log.d(TAG, "Received response: " + response.toString());
            Log.d(TAG, "Response Code: "+response.code());
            long t2 = System.nanoTime();
            Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));
        }
        return response;
    }
}
