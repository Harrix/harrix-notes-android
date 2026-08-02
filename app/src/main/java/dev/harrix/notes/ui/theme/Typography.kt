package dev.harrix.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import dev.harrix.notes.AppPreferences

/** Explicit M3 type scale (defaults pinned for consistent chrome). */
val NotesTypography = Typography()

/** Scales the whole Material type scale so [uiFontSizeSp] matches bodyMedium. */
fun notesTypography(uiFontSizeSp: Int): Typography {
    val clamped = uiFontSizeSp.coerceIn(AppPreferences.MIN_FONT_SIZE_SP, AppPreferences.MAX_FONT_SIZE_SP)
    val scale = clamped / AppPreferences.DEFAULT_UI_FONT_SIZE_SP.toFloat()
    if (scale == 1f) {
        return NotesTypography
    }
    val base = NotesTypography
    return Typography(
        displayLarge = base.displayLarge.scaled(scale),
        displayMedium = base.displayMedium.scaled(scale),
        displaySmall = base.displaySmall.scaled(scale),
        headlineLarge = base.headlineLarge.scaled(scale),
        headlineMedium = base.headlineMedium.scaled(scale),
        headlineSmall = base.headlineSmall.scaled(scale),
        titleLarge = base.titleLarge.scaled(scale),
        titleMedium = base.titleMedium.scaled(scale),
        titleSmall = base.titleSmall.scaled(scale),
        bodyLarge = base.bodyLarge.scaled(scale),
        bodyMedium = base.bodyMedium.scaled(scale),
        bodySmall = base.bodySmall.scaled(scale),
        labelLarge = base.labelLarge.scaled(scale),
        labelMedium = base.labelMedium.scaled(scale),
        labelSmall = base.labelSmall.scaled(scale),
    )
}

private fun TextStyle.scaled(scale: Float): TextStyle =
    copy(
        fontSize = fontSize.scaledSp(scale),
        lineHeight = lineHeight.scaledSp(scale),
    )

private fun TextUnit.scaledSp(scale: Float): TextUnit =
    if (type == TextUnitType.Sp) (value * scale).sp else this
