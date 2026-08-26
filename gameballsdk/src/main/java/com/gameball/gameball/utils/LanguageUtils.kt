package com.gameball.gameball.utils

import com.gameball.gameball.local.SharedPreferencesUtils
import java.util.*

object LanguageUtils {
    private val ltrLanguageCodes = listOf(
        "en", "fr", "es", "de", "pt", "pl", "it", "hu", "zh-tw", "nl", "sv", "no", "dk", "ja"
    )
    private val rtlLanguageCodes = listOf("ar")

    /**
     * Resolves the language to use, in priority order:
     * 1. Explicit per-call [override] (e.g. passed to showProfile)
     * 2. Customer preferred language (set via CustomerAttributes)
     * 3. Global preferred language (set during SDK init)
     * 4. Device locale (fallback)
     */
    @JvmStatic
    @JvmOverloads
    fun handleLanguage(override: String? = null): String {
        // Highest priority: explicit per-call override
        if (override != null && override.length == 2) {
            return override
        }

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