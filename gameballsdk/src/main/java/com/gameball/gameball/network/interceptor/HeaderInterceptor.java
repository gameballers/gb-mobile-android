package com.gameball.gameball.network.interceptor;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.utils.Constants;

import java.io.IOException;
import java.util.Locale;

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

        if (SharedPreferencesUtils.getInstance().getClientId() != null)
        {
//            builder.addHeader(Constants.APIKey,
//                    SharedPreferencesUtils.getInstance().getClientId());
            builder.addHeader("ClientId",
                    "1");

        }

        builder.addHeader(Constants.LangKey, Locale.getDefault().getLanguage());

        request = builder.build();
        return chain.proceed(request);
    }
}
