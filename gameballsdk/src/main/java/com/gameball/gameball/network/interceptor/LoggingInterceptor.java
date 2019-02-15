package com.gameball.gameball.network.interceptor;

import android.util.Log;

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
        Log.i("OkHttp",request.toString());

        long t1 = System.nanoTime();
        Log.d("OkHttp", String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));

        Response response = chain.proceed(request);

        Log.i("OkHttp",response.toString());

        Log.d("OkHttp", "Received response: " + response.toString());
        Log.i("response_code",response.code()+"");
        long t2 = System.nanoTime();
        Log.d("OkHttp", String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        return response;
    }
}
