package dev.harrix.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.harrix.notes.ui.MainScreen
import dev.harrix.notes.ui.theme.HarrixNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences(this) }
            var themeMode by remember { mutableStateOf(preferences.loadThemeMode()) }
            val darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())
            HarrixNotesTheme(darkTheme = darkTheme) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        preferences.saveThemeMode(mode)
                        themeMode = mode
                    },
                    onExitApp = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
