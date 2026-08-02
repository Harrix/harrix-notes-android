package dev.harrix.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.harrix.notes.ui.MainScreen
import dev.harrix.notes.ui.theme.HarrixNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = AppPreferences(this)
        preferences.loadAppLanguage().apply()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(preferences.loadThemeMode()) }
            var uiFontSizeSp by remember { mutableIntStateOf(preferences.loadUiFontSizeSp()) }
            var appLanguage by remember { mutableStateOf(preferences.loadAppLanguage()) }
            val darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())
            HarrixNotesTheme(
                darkTheme = darkTheme,
                uiFontSizeSp = uiFontSizeSp,
            ) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        preferences.saveThemeMode(mode)
                        themeMode = mode
                    },
                    uiFontSizeSp = uiFontSizeSp,
                    onUiFontSizeChange = { size ->
                        preferences.saveUiFontSizeSp(size)
                        uiFontSizeSp = size
                    },
                    appLanguage = appLanguage,
                    onAppLanguageChange = { language ->
                        preferences.saveAppLanguage(language)
                        appLanguage = language
                        language.apply()
                    },
                    onExitApp = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
