package dev.harrix.notes.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand seed: primary actions (#2E86B7). */
val BrandPrimary = Color(0xFF2E86B7)

/** Brand seed: success / positive accents (#35965F). */
val BrandGreen = Color(0xFF35965F)

/** Brand seed: error actions (#CC584C). */
val BrandRed = Color(0xFFCC584C)

val LightColorScheme =
    lightColorScheme(
        primary = BrandPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC5E3F4),
        onPrimaryContainer = Color(0xFF00344F),
        secondary = Color(0xFF4F616D),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2E5F3),
        onSecondaryContainer = Color(0xFF0B1E28),
        tertiary = BrandGreen,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFB9EBCF),
        onTertiaryContainer = Color(0xFF002113),
        error = BrandRed,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF5F8FA),
        onBackground = Color(0xFF171C1F),
        surface = Color(0xFFF5F8FA),
        onSurface = Color(0xFF171C1F),
        surfaceVariant = Color(0xFFDCE3E8),
        onSurfaceVariant = Color(0xFF40484D),
        outline = Color(0xFF70787E),
        outlineVariant = Color(0xFFC0C8CD),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2C3134),
        inverseOnSurface = Color(0xFFEDF1F4),
        inversePrimary = Color(0xFF8FCEF0),
        surfaceTint = BrandPrimary,
        surfaceBright = Color(0xFFF5F8FA),
        surfaceDim = Color(0xFFD6DBDE),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEFF3F6),
        surfaceContainer = Color(0xFFE9EEF1),
        surfaceContainerHigh = Color(0xFFE3E8EB),
        surfaceContainerHighest = Color(0xFFDDE2E6),
    )

val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF8FCEF0),
        onPrimary = Color(0xFF00344F),
        primaryContainer = Color(0xFF1A5F85),
        onPrimaryContainer = Color(0xFFC5E3F4),
        secondary = Color(0xFFB6C9D6),
        onSecondary = Color(0xFF20333E),
        secondaryContainer = Color(0xFF374955),
        onSecondaryContainer = Color(0xFFD2E5F3),
        tertiary = Color(0xFF7DD0A4),
        onTertiary = Color(0xFF003822),
        tertiaryContainer = Color(0xFF1B7246),
        onTertiaryContainer = Color(0xFFB9EBCF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0F1417),
        onBackground = Color(0xFFDDE2E6),
        surface = Color(0xFF0F1417),
        onSurface = Color(0xFFDDE2E6),
        surfaceVariant = Color(0xFF40484D),
        onSurfaceVariant = Color(0xFFC0C8CD),
        outline = Color(0xFF8A9297),
        outlineVariant = Color(0xFF40484D),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDDE2E6),
        inverseOnSurface = Color(0xFF2C3134),
        inversePrimary = Color(0xFF2E86B7),
        surfaceTint = Color(0xFF8FCEF0),
        surfaceBright = Color(0xFF353A3D),
        surfaceDim = Color(0xFF0F1417),
        surfaceContainerLowest = Color(0xFF0A0F12),
        surfaceContainerLow = Color(0xFF171C1F),
        surfaceContainer = Color(0xFF1B2023),
        surfaceContainerHigh = Color(0xFF252A2D),
        surfaceContainerHighest = Color(0xFF303538),
    )
