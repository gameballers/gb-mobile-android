package com.gameball.gameball.network.interceptor;

import static com.gameball.gameball.utils.Constants.TAG;

import android.util.Log;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.network.Config;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.utils.LanguageUtils;
import com.google.gson.Gson;

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
            String sessionToken = sharedPrefs.getSessionTokenPreference();
            if (sessionToken != null) {
                builder.addHeader("X-GB-TOKEN", sessionToken);

                // Switch to secure endpoint (v4.1) if sessionToken is present
                String path = request.url().encodedPath();
                if (path.contains(Config.API_V4_0)) {
                    String newPath = path.replace(Config.API_V4_0, Config.API_V4_1);
                    builder.url(request.url().newBuilder().encodedPath(newPath).build());
                }
            }

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
