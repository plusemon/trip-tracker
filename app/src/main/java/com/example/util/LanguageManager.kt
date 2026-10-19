package com.example.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "tripsplit_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun setAppLocale(context: Context, languageCode: String) { // "en" or "bn"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved

        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            val tag = currentLocales.toLanguageTags()
            if (tag.startsWith("bn")) return "bn"
        }
        return "en"
    }

    fun applySavedLocale(context: Context) {
        val saved = getSavedLanguage(context)
        val appLocale = LocaleListCompat.forLanguageTags(saved)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
