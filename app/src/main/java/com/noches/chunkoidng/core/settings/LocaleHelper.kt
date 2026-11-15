package com.noches.chunkoidng.core.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleHelper {
    private val systemDefaultLocale: Locale = Locale.getDefault()
    private val _languageFlow = MutableStateFlow("system")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun getLocale(languageCode: String): Locale {
        return when (languageCode.lowercase()) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            "en" -> Locale.ENGLISH
            else -> systemDefaultLocale
        }
    }

    fun observeLanguage(context: Context): StateFlow<String> {
        val persisted = AppPreferences(context).appLanguage
        if (_languageFlow.value != persisted) {
            _languageFlow.value = persisted
        }
        return languageFlow
    }

    fun notifyLanguageChanged(languageCode: String) {
        _languageFlow.value = languageCode
    }

    fun applyLocale(
        context: Context,
        languageCode: String,
        baseConfiguration: Configuration = context.resources.configuration
    ): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(baseConfiguration)
        config.setLocales(android.os.LocaleList(locale))
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLocaleInPlace(activity: Activity, languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        updateContextResources(activity, locale)
        activity.applicationContext?.let { appCtx ->
            if (appCtx !== activity) {
                updateContextResources(appCtx, locale)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateContextResources(target: Context, locale: Locale) {
        runCatching {
            val res = target.resources
            val config = Configuration(res.configuration)
            config.setLocales(android.os.LocaleList(locale))
            config.setLocale(locale)
            res.updateConfiguration(config, res.displayMetrics)
        }
    }

    fun localizedContext(context: Context): Context {
        val lang = AppPreferences(context).appLanguage
        return applyLocale(context, lang)
    }
}
