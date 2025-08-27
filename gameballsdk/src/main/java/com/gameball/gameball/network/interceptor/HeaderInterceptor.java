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

        SharedPreferencesUtils sharedPrefs = SharedPreferencesUtils.getInstanceOrNull();
        
        if (sharedPrefs != null && sharedPrefs.getApiKey() != null)
        {
            builder.addHeader(Constants.APIKey, sharedPrefs.getApiKey());
        }

        String langHeader = LanguageUtils.handleLanguage();
        builder.addHeader(Constants.LangKey, langHeader);

        if (sharedPrefs != null) {
            String osVersion = sharedPrefs.getOSPreference();
            String sdkVersion = sharedPrefs.getSDKPreference();
            if(osVersion != null && sdkVersion != null){
                builder.addHeader(Constants.USER_AGENT,
                        String.format("GB/%s/%s", osVersion, sdkVersion));
            }
        }

        request = builder.build();

        Log.d(TAG, request.url().toString());

        return chain.proceed(request);
    }
}
