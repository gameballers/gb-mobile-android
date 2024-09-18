package com.gameball.gameball.utils;

import com.gameball.gameball.local.SharedPreferencesUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LanguageUtils {
    private static final List<String> ltrLanguageCodes =
            Arrays.asList("en", "fr", "es", "de", "pt", "pl", "it", "hu", "zh-tw", "nl", "sv", "no", "dk", "ja");
    private static final List<String> rtlLanguageCodes = Arrays.asList("ar");

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

    public static boolean shouldHandleCloseButtonDirection(String selectedLanguage){
        String deviceLocale = Locale.getDefault().getLanguage();
        return (isRtl(deviceLocale) && isLtr(selectedLanguage)) || (isRtl(selectedLanguage) && isLtr(deviceLocale));
    }

    public static boolean isLtr(String languageCode){
        return ltrLanguageCodes.contains(languageCode);
    }

    public static boolean isRtl(String languageCode){
        return rtlLanguageCodes.contains(languageCode);
    }
}
