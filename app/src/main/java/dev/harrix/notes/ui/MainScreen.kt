package dev.harrix.notes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.harrix.notes.ui.notes.NotesViewerScreen
import dev.harrix.notes.ui.settings.SettingsScreen
import dev.harrix.notes.ui.theme.ThemeMode

@Composable
fun MainScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }
    var settingsRevision by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        NotesViewerScreen(
            onClose = onExitApp,
            onOpenSettings = { showSettings = true },
            settingsRevision = settingsRevision,
            modifier = Modifier.fillMaxSize(),
        )

        if (showSettings) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onClose = {
                    showSettings = false
                    settingsRevision += 1
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
