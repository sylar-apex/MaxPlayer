package org.app.nxplayer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.app.nxplayer.core.Keys
import org.app.nxplayer.data.AppGraph

val accentSwatches = listOf(
    0xFFE0E0E0, 0xFF3D8BF2, 0xFF00BFA5, 0xFF43A047, 0xFFFFB300, 0xFFFB8C00, 0xFFE53935,
    0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF039BE5, 0xFF6D4C41,
    0xFF546E7A, 0xFF7CB342, 0xFFC0CA33, 0xFFF4511E, 0xFF00897B, 0xFF1E88E5,
)

@Composable
fun NxTheme(font: androidx.compose.ui.text.font.FontFamily = Rubik, content: @Composable () -> Unit) {
    val version by AppGraph.settings.version.collectAsState()
    version
    val mode = AppGraph.settings[Keys.appTheme]
    val accent = Color(AppGraph.settings[Keys.accentColor])
    val dark = when (mode) {
        "light" -> false
        "dark", "black" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) {
        val bg = if (mode == "black") Color.Black else Color(0xFF16202B)
        val surface = if (mode == "black") Color(0xFF0C0C0C) else Color(0xFF29333D)
        darkColorScheme(
            primary = accent, secondary = accent, onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White, background = bg, surface = bg,
            surfaceVariant = surface, surfaceContainer = surface, surfaceContainerHigh = surface,
            onBackground = Color(0xFFDADDE4), onSurface = Color(0xFFDADDE4), onSurfaceVariant = Color(0xFF9AA6B2),
        )
    } else {
        lightColorScheme(primary = accent, secondary = accent, background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFF1F3F5))
    }
    val base = androidx.compose.material3.Typography()
    val typography = base.copy(
        displayLarge = base.displayLarge.styled(font), displayMedium = base.displayMedium.styled(font), displaySmall = base.displaySmall.styled(font),
        headlineLarge = base.headlineLarge.styled(font), headlineMedium = base.headlineMedium.styled(font), headlineSmall = base.headlineSmall.styled(font),
        titleLarge = base.titleLarge.styled(font), titleMedium = base.titleMedium.styled(font), titleSmall = base.titleSmall.styled(font),
        bodyLarge = base.bodyLarge.styled(font), bodyMedium = base.bodyMedium.styled(font), bodySmall = base.bodySmall.styled(font),
        labelLarge = base.labelLarge.styled(font), labelMedium = base.labelMedium.styled(font), labelSmall = base.labelSmall.styled(font),
    )
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}

private fun androidx.compose.ui.text.TextStyle.styled(font: androidx.compose.ui.text.font.FontFamily) =
    if (font == Rubik) copy(fontFamily = font, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified) else copy(fontFamily = font)

private fun variable(res: Int, weight: Int) = androidx.compose.ui.text.font.Font(
    res, androidx.compose.ui.text.font.FontWeight(weight),
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(weight)),
)

val Mulish = androidx.compose.ui.text.font.FontFamily(listOf(400, 500, 600, 700, 800).map { variable(org.app.nxplayer.R.font.mulish, it) })
val Rubik = androidx.compose.ui.text.font.FontFamily(listOf(400, 500, 600, 700).map { variable(org.app.nxplayer.R.font.rubik, it) })
