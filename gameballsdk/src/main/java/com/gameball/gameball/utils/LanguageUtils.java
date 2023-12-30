package com.gameball.gameball.utils;

import com.gameball.gameball.local.SharedPreferencesUtils;

import java.util.Locale;

public class LanguageUtils {
    public static String HandleLanguage(){
        String language = SharedPreferencesUtils.getInstance().getPlayerPreferredLanguage();
        if(language == null || language.length() != 2){
            language = SharedPreferencesUtils.getInstance().getGlobalPreferredLanguage();

            if(language == null || language.length() != 2){
                language = Locale.getDefault().getLanguage();
            }
        }
        return language;
    }
}
