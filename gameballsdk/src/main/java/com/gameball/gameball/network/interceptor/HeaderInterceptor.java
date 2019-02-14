package com.gameball.gameball.network.interceptor;

import com.gameball.gameball.local.LocalDataSource;
import com.gameball.gameball.utils.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor
{
    @Override
    public Response intercept(Chain chain) throws IOException
    {
        Request request = chain.request();

        Request.Builder builder = request.newBuilder();

        if (LocalDataSource.getInstance().clientId != null)
            builder.addHeader(Constants.APIKey,
                    LocalDataSource.getInstance().clientId);

        request = builder.build();
        return chain.proceed(request);
    }
}
