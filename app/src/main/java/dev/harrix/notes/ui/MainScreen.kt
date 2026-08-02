package dev.harrix.notes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.harrix.notes.ui.about.AboutScreen
import dev.harrix.notes.ui.notes.NotesViewerScreen
import dev.harrix.notes.ui.settings.SettingsScreen
import dev.harrix.notes.ui.theme.ThemeMode

@Composable
fun MainScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiFontSizeSp: Int,
    onUiFontSizeChange: (Int) -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Survive Activity recreation (e.g. landscape rotation).
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var settingsRevision by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        NotesViewerScreen(
            onClose = onExitApp,
            onOpenSettings = { showSettings = true },
            onOpenAbout = { showAbout = true },
            settingsRevision = settingsRevision,
            modifier = Modifier.fillMaxSize(),
        )

        if (showSettings) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                uiFontSizeSp = uiFontSizeSp,
                onUiFontSizeChange = onUiFontSizeChange,
                onClose = {
                    showSettings = false
                    settingsRevision += 1
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (showAbout) {
            AboutScreen(
                onClose = { showAbout = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
