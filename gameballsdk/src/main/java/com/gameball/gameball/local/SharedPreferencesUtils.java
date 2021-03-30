package com.gameball.gameball.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.gameball.gameball.model.response.ClientBotSettings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class SharedPreferencesUtils {

    private static final String PREF_FILE_NAME = "GameBallSharedPreference";
    private static SharedPreferencesUtils mSharedPreferencesUtils;
    private SharedPreferences pref;
    private Context context;
    private Gson gson;

    private SharedPreferencesUtils(Context context, Gson gson) {
        this.pref = context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        this.context = context;
        this.gson = gson;
    }

    public static SharedPreferencesUtils getInstance() {
        return mSharedPreferencesUtils;
    }

    public static SharedPreferencesUtils init(Context context, Gson gson) {
        if (mSharedPreferencesUtils == null)
            mSharedPreferencesUtils = new SharedPreferencesUtils(context, gson);

        return mSharedPreferencesUtils;
    }

    private SharedPreferences getPref() {
        return pref;
    }

    // Primitive
    private void putInt(String key, int value) {
        pref.edit().putInt(key, value).apply();
    }

    private int getInt(String key, int def) {
        return pref.getInt(key, def);
    }

    private void putLong(String key, long value) {
        pref.edit().putLong(key, value).apply();
    }

    private long getLong(String key, long def) {
        return pref.getLong(key, def);
    }

    private void putFloat(String key, float value) {
        pref.edit().putFloat(key, value).apply();
    }

    private float getFloat(String key, float def) {
        return pref.getFloat(key, def);
    }

    private void putBoolean(String key, boolean value) {
        pref.edit().putBoolean(key, value).apply();
    }

    private boolean getBoolean(String key, boolean def) {
        return pref.getBoolean(key, def);
    }

    private void putString(String key, String value) {
        pref.edit().putString(key, value).apply();
    }

    private String getString(String key, String def) {
        return pref.getString(key, def);
    }

    private Set<String> getStringSet(String key, HashSet<String> def) {
        return pref.getStringSet(key, def);
    }

    private void putStringSet(String key, HashSet<String> value) {
        pref.edit().putStringSet(key, value).apply();
    }

    // Date
    private void putDate(String key, Date date) {
        pref.edit().putLong(key, date.getTime()).apply();
    }

    private Date getDate(String key) {
        return new Date(pref.getLong(key, 0));
    }

    // Gson
    private <T> void putObject(String key, T t) {
        pref.edit().putString(key, gson.toJson(t)).apply();
    }

    private <T> T getObject(String key, TypeToken<T> typeToken) {
        String objectString = pref.getString(key, null);
        if (objectString != null) {
            return gson.fromJson(objectString, typeToken.getType());
        }
        return null;
    }

    public void clearData() {
        pref.edit().clear().apply();
    }

    public void putDeviceToken(String deviceToken) {
        putString(PreferencesContract.DEVICE_TOKEN, deviceToken);
    }

    public void putPlayerUniqueId(String playerUniqueId) {
        putString(PreferencesContract.PLAYER_UNIQUE_ID, playerUniqueId);
    }

    public void putClientId(String clientId) {
        putString(PreferencesContract.API_KEY, clientId);
    }

    public String getDeviceToken() {
        return getString(PreferencesContract.DEVICE_TOKEN, null);
    }

    public String getPlayerUniqueId() {
        return getString(PreferencesContract.PLAYER_UNIQUE_ID, null);
    }

    public String getClientId() {
        return getString(PreferencesContract.API_KEY, null);
    }

    public void putClientBotSettings(ClientBotSettings clientBotSettings)
    {
        putString(PreferencesContract.CLIENT_BOT_SETTINGS, gson.toJson(clientBotSettings));
    }

    public ClientBotSettings getClientBotSettings()
    {
        ClientBotSettings clientBotSettings = gson.fromJson(
                getString(PreferencesContract.CLIENT_BOT_SETTINGS,null), ClientBotSettings.class);
        return clientBotSettings;
    }

    public void putPlayerRefferalLink(String playerReferralLink)
    {
        putString(PreferencesContract.PLAYER_DYNAMIC_LINK, playerReferralLink);
    }

    public String getPlayerReferralLink()
    {
        return getString(PreferencesContract.PLAYER_DYNAMIC_LINK, null);
    }

    private static final class PreferencesContract {

        private static final String PLAYER_UNIQUE_ID = "PLAYER_UNIQUE_ID";
        private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
        private static final String API_KEY = "API_KEY";
        private static final String CLIENT_BOT_SETTINGS = "BOT_SETTINGS";
        private static final String PLAYER_TYPE_ID = "PLAYER_TYPE_ID";
        private static final String PLAYER_DYNAMIC_LINK = "PLAYER_DYNAMIC_LINK";
    }

}
