package dev.harrix.notes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {
    private var pendingOpenUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = AppPreferences(this)
        preferences.loadAppLanguage().apply()
        super.onCreate(savedInstanceState)
        consumeOpenIntent(intent)
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
                    pendingOpenUri = pendingOpenUri,
                    onPendingOpenUriConsumed = { pendingOpenUri = null },
                    onExitApp = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOpenIntent(intent)
    }

    private fun consumeOpenIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        if (intent.action == Intent.ACTION_MAIN) {
            return
        }
        val uri = NotesOpenIntent.extractUri(intent) ?: return
        if (!NotesOpenIntent.isLikelyMarkdown(this, uri, intent)) {
            return
        }
        NotesOpenIntent.takeReadWritePermissionIfPossible(this, intent, uri)
        pendingOpenUri = uri
    }
}
