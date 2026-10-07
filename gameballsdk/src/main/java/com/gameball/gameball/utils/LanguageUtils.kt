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
     * The first one set wins; an unsupported code resolves to "en".
     */
    @JvmStatic
    @JvmOverloads
    fun handleLanguage(override: String? = null): String {
        // Highest priority: explicit per-call override
        if (!override.isNullOrEmpty()) {
            return normalizeLanguage(override)
        }

        val sharedPrefs = SharedPreferencesUtils.getInstanceOrNull()

        // First try customer preferred language
        val customerLanguage = sharedPrefs?.getCustomerPreferredLanguage()
        if (!customerLanguage.isNullOrEmpty()) {
            return normalizeLanguage(customerLanguage)
        }
        
        // Then try global preferred language
        val globalLanguage = sharedPrefs?.getGlobalPreferredLanguage()
        if (!globalLanguage.isNullOrEmpty()) {
            return normalizeLanguage(globalLanguage)
        }
        
        // Fall back to device locale
        return normalizeLanguage(Locale.getDefault().language)
    }

    /** Returns [languageCode] trimmed and lowercased if it's supported, otherwise "en". */
    @JvmStatic
    fun normalizeLanguage(languageCode: String): String {
        val code = languageCode.trim().lowercase(Locale.ROOT)
        return if (code in ltrLanguageCodes || code in rtlLanguageCodes) code else "en"
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