package org.app.maxplayerpro.appcore.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.settings.PlayerKeys
import org.app.kmediaplayer.ui.settings.settingsVersion
import org.app.kmediaplayer.ui.theme.withFont
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.mulish
import org.app.maxplayerpro.appres.resources.rubik
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource


@Composable
private fun variableFamily(res: FontResource, weights: List<Int>): FontFamily =
    FontFamily(weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) })

/** Rubik, the app's typeface. */
@Composable
fun rubikFontFamily(): FontFamily = variableFamily(Res.font.rubik, listOf(400, 500, 600, 700))

/** Mulish, used by the video player. */
@Composable
fun mulishFontFamily(): FontFamily = variableFamily(Res.font.mulish, listOf(400, 500, 600, 700, 800))

/** App theme: dark, light, AMOLED black or following the system, with the chosen accent colour. */
@Composable
fun MaxTheme(font: FontFamily? = null, content: @Composable () -> Unit) {
    settingsVersion()
    val settings = KMediaPlayer.settings
    val mode = settings[AppKeys.appTheme]
    val accent = Color(settings[PlayerKeys.accentColor])
    val dark = when (mode) {
        "light" -> false
        "dark", "black" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) {
        val bg = if (mode == "black") MaxColors.blackBackground else MaxColors.darkBackground
        val surface = if (mode == "black") MaxColors.blackSurface else MaxColors.darkSurface
        darkColorScheme(
            primary = accent, secondary = accent, onPrimary = if (accent.luminance() > 0.5f) MaxColors.onLightAccent else MaxColors.onDarkAccent, background = bg, surface = bg,
            surfaceVariant = surface, surfaceContainer = surface, surfaceContainerHigh = surface,
            onBackground = MaxColors.darkOnBackground, onSurface = MaxColors.darkOnBackground, onSurfaceVariant = MaxColors.darkOnSurfaceVariant,
        )
    } else {
        lightColorScheme(primary = accent, secondary = accent, background = MaxColors.lightBackground, surface = MaxColors.lightBackground, surfaceVariant = MaxColors.lightSurfaceVariant)
    }
    val family = font ?: rubikFontFamily()
    val typography = Typography().withFont(family, keepLetterSpacing = font != null)
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
