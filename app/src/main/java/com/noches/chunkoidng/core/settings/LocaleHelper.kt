package com.noches.chunkoidng.core.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun getLocale(languageCode: String): Locale {
        return when (languageCode.lowercase()) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }

    fun applyLocale(context: Context, languageCode: String): Context {
        if (languageCode == "system") {
            return context
        }
        val locale = getLocale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocales(android.os.LocaleList(locale))
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
