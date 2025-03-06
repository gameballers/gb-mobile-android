package com.gameball.gameball.network.interceptor;

import static com.gameball.gameball.utils.Constants.TAG;

import android.util.Log;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.LanguageUtils;

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

        if (SharedPreferencesUtils.getInstance().getApiKey() != null)
        {
            builder.addHeader(Constants.APIKey,
                    SharedPreferencesUtils.getInstance().getApiKey());

        }

        String langHeader = LanguageUtils.HandleLanguage();

        builder.addHeader(Constants.LangKey, langHeader);

        String osVersion = SharedPreferencesUtils.getInstance().getOSPreference();
        String sdkVersion = SharedPreferencesUtils.getInstance().getSDKPreference();
        if(osVersion != null && sdkVersion != null){
            builder.addHeader(Constants.USER_AGENT,
                    String.format("GB/%s/%s", osVersion, sdkVersion));
        }

        request = builder.build();

        Log.d(TAG, request.url().toString());

        return chain.proceed(request);
    }
}
