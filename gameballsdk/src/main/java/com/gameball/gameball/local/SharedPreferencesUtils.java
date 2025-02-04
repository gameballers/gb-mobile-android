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

    //Use with caution!
    public void remove(String prefKey){
        pref.edit().remove(prefKey).apply();
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

    public void putCustomerId(String customerId) {
        putString(PreferencesContract.CUSTOMER_ID, customerId);
    }

    public void putApiKey(String apiKey) {
        putString(PreferencesContract.API_KEY, apiKey);
    }

    public String getDeviceToken() {
        return getString(PreferencesContract.DEVICE_TOKEN, null);
    }

    public String getCustomerId() {
        return getString(PreferencesContract.CUSTOMER_ID, null);
    }

    public String getApiKey() {
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

    public void putCustomerRefferalLink(String customerReferralLink)
    {
        putString(PreferencesContract.CUSTOMER_DYNAMIC_LINK, customerReferralLink);
    }

    public String getCustomerReferralLink()
    {
        return getString(PreferencesContract.CUSTOMER_DYNAMIC_LINK, null);
    }

    public void putGlobalPreferredLanguage(String language) {
        putString(PreferencesContract.GLOBAL_PREFERRED_LANGUAGE, language);
    }

    public String getGlobalPreferredLanguage() {
        return getString(PreferencesContract.GLOBAL_PREFERRED_LANGUAGE, null);
    }

    public void putPlatformPreference(String platform){
        putString(PreferencesContract.PLATFORM_PREFERENCE, platform);
    }

    public String getPlatformPreference(){
        return getString(PreferencesContract.PLATFORM_PREFERENCE, null);
    }

    public void putShopPreference(String shop){
        putString(PreferencesContract.SHOP_PREFERENCE, shop);
    }

    public String getShopPreference(){
        return getString(PreferencesContract.SHOP_PREFERENCE, null);
    }

    public void putSDKPreference(String sdVersion){
        putString(PreferencesContract.SDK_VERSION_PREFERENCE, sdVersion);
    }

    public String getSDKPreference(){
        return getString(PreferencesContract.SDK_VERSION_PREFERENCE, null);
    }

    public void putOSPreference(String osVersion){
        putString(PreferencesContract.OS_VERSION_PREFERENCE, osVersion);
    }

    public String getOSPreference(){
        return getString(PreferencesContract.OS_VERSION_PREFERENCE, null);
    }

    public void putOpenDetailPreference(String openDetail){
        putString(PreferencesContract.OPEN_DETAIL_PREFERENCE, openDetail);
    }

    public String getOpenDetailPreference(){
        return getString(PreferencesContract.OPEN_DETAIL_PREFERENCE, null);
    }
    public void removeOpenDetailPreference(){
        remove(PreferencesContract.OPEN_DETAIL_PREFERENCE);
    }

    public void putHideNavigationPreference(Boolean hideNavigation){
        if(hideNavigation != null)
            putString(PreferencesContract.HIDE_NAVIGATION_PREFERENCE, hideNavigation.toString());
        else putString(PreferencesContract.HIDE_NAVIGATION_PREFERENCE, null);
    }

    public String getHideNavigationPreference(){
        return getString(PreferencesContract.HIDE_NAVIGATION_PREFERENCE, null);
    }

    public void removeHideNavigationPreference(){
        remove(PreferencesContract.HIDE_NAVIGATION_PREFERENCE);
    }

    public String getCustomerPreferredLanguage(){
        return getString(PreferencesContract.CUSTOMER_PREFERRED_LANGUAGE, null);
    }

    public void putCustomerPreferredLanguage(String preferredLanguage){
        putString(PreferencesContract.CUSTOMER_PREFERRED_LANGUAGE, preferredLanguage);
    }

    private static final class PreferencesContract {

        private static final String CUSTOMER_ID = "CUSTOMER_ID";
        private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
        private static final String API_KEY = "API_KEY";
        private static final String CLIENT_BOT_SETTINGS = "BOT_SETTINGS";
        private static final String CUSTOMER_TYPE_ID = "CUSTOMER_TYPE_ID";
        private static final String CUSTOMER_DYNAMIC_LINK = "CUSTOMER_DYNAMIC_LINK";
        private static final String GLOBAL_PREFERRED_LANGUAGE = "GLOBAL_PREFERRED_LANGUAGE";
        private static final String PLATFORM_PREFERENCE = "PLATFORM_PREFERENCE";
        private static final String SHOP_PREFERENCE = "SHOP_PREFERENCE";
        private static final String OS_VERSION_PREFERENCE = "OS_VERSION_PREFERENCE";
        private static final String SDK_VERSION_PREFERENCE = "SDK_VERSION_PREFERENCE";
        private static final String OPEN_DETAIL_PREFERENCE = "OPEN_DETAIL_PREFERENCE";
        private static final String HIDE_NAVIGATION_PREFERENCE = "HIDE_NAVIGATION_PREFERENCE";
        private static final String CUSTOMER_PREFERRED_LANGUAGE = "CUSTOMER_PREFERRED_LANGUAGE";
    }

}
