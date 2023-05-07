package com.gameball.gameball.network.interceptor;

import android.util.Log;

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

        if (SharedPreferencesUtils.getInstance().getApiKey() != null)
        {
            builder.addHeader(Constants.APIKey,
                    SharedPreferencesUtils.getInstance().getApiKey());

        }

        builder.addHeader(Constants.LangKey, Locale.getDefault().getLanguage());

        String osVersion = SharedPreferencesUtils.getInstance().getOSPreference();
        String sdkVersion = SharedPreferencesUtils.getInstance().getSDKPreference();
        if(osVersion != null && sdkVersion != null){
            builder.addHeader(Constants.USER_AGENT,
                    String.format("GB/%s/%s", osVersion, sdkVersion));
        }

        request = builder.build();

        Log.d("OkHttp", request.url().toString());

        return chain.proceed(request);
    }
}
