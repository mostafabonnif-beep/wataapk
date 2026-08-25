package com.elwataniatv.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Single source of truth for the app language.
 *
 * Arabic is the product default. Once the user explicitly chooses a language,
 * the choice is kept locally so a device configured in English cannot silently
 * switch the application back to English after an Activity restart.
 */
object AppLanguage {
    const val ARABIC_TAG = "ar"
    const val FRENCH_TAG = "fr"
    const val ENGLISH_TAG = "en"

    private const val PREFERENCES_NAME = "app_preferences"
    private const val LANGUAGE_KEY = "app_language"

    /**
     * Applies the first-run/default language before the first Activity is shown.
     * On Android 13+ the platform locale API is authoritative; on older Android
     * versions the Activity base context is wrapped and the legacy configuration
     * is updated as a compatibility fallback.
     */
    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val savedLanguage = preferences.getString(LANGUAGE_KEY, null)?.let(::normalize)
        val language = savedLanguage ?: detectInitialLanguage(context).also {
            preferences.edit().putString(LANGUAGE_KEY, it).apply()
        }

        applyToApplication(context, language)
    }

    /** Returns the persisted language, defaulting to Arabic on a fresh install. */
    fun preferredLanguage(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(LANGUAGE_KEY, null)?.let(::normalize)
            ?: detectInitialLanguage(context)
    }

    /** Applies and persists the language selected from Settings. */
    fun applyAppLanguage(activity: Activity, languageTag: String) {
        val language = normalize(languageTag)
        activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, language)
            .apply()

        val currentLanguage = currentApplicationLanguage(activity)
        applyToApplication(activity, language)

        // LocaleManager recreates Activities automatically when its value changes.
        // The explicit recreate is needed only for the legacy configuration path.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && currentLanguage != language) {
            activity.recreate()
        }
    }

    /** Wraps an Activity base context so resources are localized before Compose starts. */
    fun wrapContext(base: Context): Context {
        val locale = Locale.forLanguageTag(preferredLanguage(base))
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }

    private fun detectInitialLanguage(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val platformLocales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!platformLocales.isEmpty) {
                return normalize(platformLocales[0].language)
            }
        }

        // The channel is Arabic-first. Device language must not silently select
        // English on a fresh install; English is available through Settings.
        return ARABIC_TAG
    }

    private fun currentApplicationLanguage(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return locales.takeIf { !it.isEmpty }?.get(0)?.language?.let(::normalize)
        }
        return context.resources.configuration.locales[0].language
            .takeIf { it.isNotBlank() }
            ?.let(::normalize)
    }

    private fun applyToApplication(context: Context, language: String) {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val current = localeManager.applicationLocales
            if (current.toLanguageTags() != language) {
                localeManager.applicationLocales = LocaleList.forLanguageTags(language)
            }
            return
        }

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    private fun normalize(languageTag: String): String {
        val normalized = languageTag.trim().lowercase(Locale.ROOT)
        return when {
            normalized.startsWith(ARABIC_TAG) -> ARABIC_TAG
            normalized.startsWith(FRENCH_TAG) -> FRENCH_TAG
            normalized.startsWith(ENGLISH_TAG) -> ENGLISH_TAG
            else -> ARABIC_TAG
        }
    }
}

/** Backward-compatible top-level API used by MainActivity and Compose screens. */
fun applyAppLanguage(activity: Activity, languageTag: String) =
    AppLanguage.applyAppLanguage(activity, languageTag)
