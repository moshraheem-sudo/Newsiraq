package com.example.data

import android.content.Context

class SettingsPreferences(context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences("iraq_news_hub_settings", Context.MODE_PRIVATE)

  fun load(): AppSettings =
    AppSettings(
      themeMode = ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key)),
      language = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.ARABIC.code)),
    )

  fun saveThemeMode(mode: ThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
  }

  fun saveLanguage(language: AppLanguage) {
    prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
  }

  companion object {
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "language"
  }
}
