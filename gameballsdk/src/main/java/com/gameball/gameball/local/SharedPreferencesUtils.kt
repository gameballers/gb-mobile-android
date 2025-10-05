package com.gameball.gameball.local

import android.content.Context
import android.content.SharedPreferences
import com.gameball.gameball.model.response.ClientBotSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class SharedPreferencesUtils private constructor(
    private val context: Context,
    private val gson: Gson
) {
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_FILE_NAME = "GameBallSharedPreference"
        
        @Volatile
        private var INSTANCE: SharedPreferencesUtils? = null
        
        @JvmStatic
        fun getInstance(): SharedPreferencesUtils {
            return INSTANCE ?: throw IllegalStateException(
                "SharedPreferencesUtils not initialized. Call init() first."
            )
        }
        
        @JvmStatic
        fun getInstanceOrNull(): SharedPreferencesUtils? = INSTANCE
        
        @JvmStatic
        @Synchronized
        fun init(context: Context, gson: Gson): SharedPreferencesUtils {
            return INSTANCE ?: SharedPreferencesUtils(context.applicationContext, gson).also { INSTANCE = it }
        }
        
        @JvmStatic
        fun isInitialized(): Boolean = INSTANCE != null
    }

    fun remove(prefKey: String) {
        try {
            pref.edit().remove(prefKey).apply()
        } catch (e: Exception) {
            // Log error but don't crash - graceful degradation
        }
    }

    private fun putInt(key: String, value: Int) {
        pref.edit().putInt(key, value).apply()
    }

    private fun getInt(key: String, defaultValue: Int): Int {
        return try {
            pref.getInt(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun putLong(key: String, value: Long) {
        pref.edit().putLong(key, value).apply()
    }

    private fun getLong(key: String, defaultValue: Long): Long {
        return try {
            pref.getLong(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun putFloat(key: String, value: Float) {
        pref.edit().putFloat(key, value).apply()
    }

    private fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            pref.getFloat(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun putBoolean(key: String, value: Boolean) {
        pref.edit().putBoolean(key, value).apply()
    }

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            pref.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun putString(key: String, value: String?) {
        pref.edit().putString(key, value).apply()
    }

    private fun getString(key: String, defaultValue: String? = null): String? {
        return try {
            pref.getString(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getStringSet(key: String, defaultValue: HashSet<String>): Set<String>? {
        return try {
            pref.getStringSet(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun putStringSet(key: String, value: HashSet<String>) {
        pref.edit().putStringSet(key, value).apply()
    }

    private fun putDate(key: String, date: Date) {
        pref.edit().putLong(key, date.time).apply()
    }

    private fun getDate(key: String): Date {
        return Date(pref.getLong(key, 0))
    }

    private fun <T> putObject(key: String, obj: T) {
        pref.edit().putString(key, gson.toJson(obj)).apply()
    }

    private fun <T> getObject(key: String, typeToken: TypeToken<T>): T? {
        return try {
            val objectString = pref.getString(key, null)
            if (objectString != null) {
                gson.fromJson(objectString, typeToken.type)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun clearData() {
        try {
            pref.edit().clear().apply()
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    // Public API methods with null safety
    fun putDeviceToken(deviceToken: String?) {
        putString(PreferencesContract.DEVICE_TOKEN, deviceToken)
    }

    fun putCustomerId(customerId: String?) {
        putString(PreferencesContract.CUSTOMER_ID, customerId)
    }

    fun putApiKey(apiKey: String?) {
        putString(PreferencesContract.API_KEY, apiKey)
    }

    fun getDeviceToken(): String? = getString(PreferencesContract.DEVICE_TOKEN)

    fun getCustomerId(): String? = getString(PreferencesContract.CUSTOMER_ID)

    fun getApiKey(): String? = getString(PreferencesContract.API_KEY)

    fun putClientBotSettings(clientBotSettings: ClientBotSettings?) {
        if (clientBotSettings != null) {
            putString(PreferencesContract.CLIENT_BOT_SETTINGS, gson.toJson(clientBotSettings))
        } else {
            putString(PreferencesContract.CLIENT_BOT_SETTINGS, null)
        }
    }

    fun getClientBotSettings(): ClientBotSettings? {
        return try {
            val settingsJson = getString(PreferencesContract.CLIENT_BOT_SETTINGS)
            if (settingsJson != null) {
                gson.fromJson(settingsJson, ClientBotSettings::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun putGlobalPreferredLanguage(language: String?) {
        putString(PreferencesContract.GLOBAL_PREFERRED_LANGUAGE, language)
    }

    fun getGlobalPreferredLanguage(): String? = getString(PreferencesContract.GLOBAL_PREFERRED_LANGUAGE)

    fun putPlatformPreference(platform: String?) {
        putString(PreferencesContract.PLATFORM_PREFERENCE, platform)
    }

    fun getPlatformPreference(): String? = getString(PreferencesContract.PLATFORM_PREFERENCE)

    fun putShopPreference(shop: String?) {
        putString(PreferencesContract.SHOP_PREFERENCE, shop)
    }

    fun getShopPreference(): String? = getString(PreferencesContract.SHOP_PREFERENCE)

    fun putSDKPreference(sdkVersion: String?) {
        putString(PreferencesContract.SDK_VERSION_PREFERENCE, sdkVersion)
    }

    fun getSDKPreference(): String? = getString(PreferencesContract.SDK_VERSION_PREFERENCE)

    fun putOSPreference(osVersion: String?) {
        putString(PreferencesContract.OS_VERSION_PREFERENCE, osVersion)
    }

    fun getOSPreference(): String? = getString(PreferencesContract.OS_VERSION_PREFERENCE)

    fun putOpenDetailPreference(openDetail: String?) {
        putString(PreferencesContract.OPEN_DETAIL_PREFERENCE, openDetail)
    }

    fun getOpenDetailPreference(): String? = getString(PreferencesContract.OPEN_DETAIL_PREFERENCE)

    fun removeOpenDetailPreference() {
        remove(PreferencesContract.OPEN_DETAIL_PREFERENCE)
    }

    fun putHideNavigationPreference(hideNavigation: Boolean?) {
        val value = hideNavigation?.toString()
        putString(PreferencesContract.HIDE_NAVIGATION_PREFERENCE, value)
    }

    fun getHideNavigationPreference(): String? = getString(PreferencesContract.HIDE_NAVIGATION_PREFERENCE)

    fun removeHideNavigationPreference() {
        remove(PreferencesContract.HIDE_NAVIGATION_PREFERENCE)
    }

    fun getCustomerPreferredLanguage(): String? = getString(PreferencesContract.CUSTOMER_PREFERRED_LANGUAGE)

    fun putCustomerPreferredLanguage(preferredLanguage: String?) {
        putString(PreferencesContract.CUSTOMER_PREFERRED_LANGUAGE, preferredLanguage)
    }

    fun removeSessionTokenPreference() {
        remove(PreferencesContract.GB_TOKEN_PREFERENCE)
    }

    fun putSessionTokenPreference(sessionToken: String?) {
        putString(PreferencesContract.GB_TOKEN_PREFERENCE, sessionToken)
    }

    fun getSessionTokenPreference(): String? = getString(PreferencesContract.GB_TOKEN_PREFERENCE)

    private object PreferencesContract {
        const val CUSTOMER_ID = "CUSTOMER_ID"
        const val DEVICE_TOKEN = "DEVICE_TOKEN"
        const val API_KEY = "API_KEY"
        const val CLIENT_BOT_SETTINGS = "BOT_SETTINGS"
        const val CUSTOMER_TYPE_ID = "CUSTOMER_TYPE_ID"
        const val GLOBAL_PREFERRED_LANGUAGE = "GLOBAL_PREFERRED_LANGUAGE"
        const val PLATFORM_PREFERENCE = "PLATFORM_PREFERENCE"
        const val SHOP_PREFERENCE = "SHOP_PREFERENCE"
        const val OS_VERSION_PREFERENCE = "OS_VERSION_PREFERENCE"
        const val SDK_VERSION_PREFERENCE = "SDK_VERSION_PREFERENCE"
        const val OPEN_DETAIL_PREFERENCE = "OPEN_DETAIL_PREFERENCE"
        const val HIDE_NAVIGATION_PREFERENCE = "HIDE_NAVIGATION_PREFERENCE"
        const val CUSTOMER_PREFERRED_LANGUAGE = "CUSTOMER_PREFERRED_LANGUAGE"
        const val GB_TOKEN_PREFERENCE = "GB_TOKEN_PREFERENCE"
    }
}