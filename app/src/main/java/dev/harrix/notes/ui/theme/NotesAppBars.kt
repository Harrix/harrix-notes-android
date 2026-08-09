package dev.harrix.notes.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Scaffold content insets that leave the status bar to the top chrome / [androidx.compose.material3.TopAppBar].
 *
 * Using full [WindowInsets.safeDrawing] on Scaffold pushes the app bar below the status bar, so
 * the clock strip keeps the window/background color while the bar uses a different surface.
 */
@Composable
fun notesScaffoldContentWindowInsets(): WindowInsets = WindowInsets.safeDrawing.only(
    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
)

/** Status-bar insets owned by the top app bar (not by Scaffold). */
@Composable
fun notesTopAppBarWindowInsets(): WindowInsets = WindowInsets.statusBars

/** Top app bar colors matching the status-bar strip under edge-to-edge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notesTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    scrolledContainerColor = MaterialTheme.colorScheme.surface,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)

/** Scaffold fill color matching [notesTopAppBarColors] / the status bar. */
@Composable
fun notesScaffoldContainerColor(): Color = MaterialTheme.colorScheme.surface
