package com.gameball.gameball.utils

import com.gameball.gameball.local.SharedPreferencesUtils
import java.util.*

object LanguageUtils {
    private val ltrLanguageCodes = listOf(
        "en", "fr", "es", "de", "pt", "pl", "it", "hu", "zh-tw", "nl", "sv", "no", "dk", "ja"
    )
    private val rtlLanguageCodes = listOf("ar")

    @JvmStatic
    fun handleLanguage(): String {
        val sharedPrefs = SharedPreferencesUtils.getInstanceOrNull()
        
        // First try customer preferred language
        val customerLanguage = sharedPrefs?.getCustomerPreferredLanguage()
        if (customerLanguage != null && customerLanguage.length == 2) {
            return customerLanguage
        }
        
        // Then try global preferred language
        val globalLanguage = sharedPrefs?.getGlobalPreferredLanguage()
        if (globalLanguage != null && globalLanguage.length == 2) {
            return globalLanguage
        }
        
        // Fall back to device locale
        return Locale.getDefault().language
    }

    @JvmStatic
    fun shouldHandleCloseButtonDirection(selectedLanguage: String): Boolean {
        val deviceLocale = Locale.getDefault().language
        return (isRtl(deviceLocale) && isLtr(selectedLanguage)) || 
               (isRtl(selectedLanguage) && isLtr(deviceLocale))
    }

    @JvmStatic
    fun isLtr(languageCode: String): Boolean = ltrLanguageCodes.contains(languageCode)

    @JvmStatic
    fun isRtl(languageCode: String): Boolean = rtlLanguageCodes.contains(languageCode)
}