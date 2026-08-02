package dev.harrix.notes

import android.content.Context
import dev.harrix.notes.ui.theme.AppLanguage
import dev.harrix.notes.ui.theme.ThemeMode

/** App-wide SharedPreferences (appearance and similar). */
class AppPreferences(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadThemeMode(): ThemeMode = ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, ThemeMode.System.name))

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun loadUiFontSizeSp(): Int = prefs
        .getInt(KEY_UI_FONT_SIZE_SP, DEFAULT_UI_FONT_SIZE_SP)
        .coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)

    fun saveUiFontSizeSp(value: Int) {
        prefs.edit().putInt(KEY_UI_FONT_SIZE_SP, value.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)).apply()
    }

    fun loadAppLanguage(): AppLanguage = AppLanguage.fromStorage(prefs.getString(KEY_APP_LANGUAGE, null))

    fun saveAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
    }

    fun resetAppearanceToDefaults() {
        prefs
            .edit()
            .remove(KEY_THEME_MODE)
            .remove(KEY_UI_FONT_SIZE_SP)
            .remove(KEY_APP_LANGUAGE)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_UI_FONT_SIZE_SP = "ui_font_size_sp"
        private const val KEY_APP_LANGUAGE = "app_language"

        const val DEFAULT_UI_FONT_SIZE_SP = 14
        const val MIN_FONT_SIZE_SP = 10
        const val MAX_FONT_SIZE_SP = 28
    }
}
