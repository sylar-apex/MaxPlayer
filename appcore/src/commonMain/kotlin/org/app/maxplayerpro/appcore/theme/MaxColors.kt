package org.app.maxplayerpro.appcore.theme

import androidx.compose.ui.graphics.Color

/** Every fixed colour used by the app's screens. Theme-dependent colours come from MaterialTheme.colorScheme. */
object MaxColors {
    // App theme
    val darkBackground = Color(0xFF16202B)
    val darkSurface = Color(0xFF29333D)
    val blackBackground = Color(0xFF000000)
    val blackSurface = Color(0xFF0C0C0C)
    val darkOnBackground = Color(0xFFDADDE4)
    val darkOnSurfaceVariant = Color(0xFF9AA6B2)
    val lightBackground = Color(0xFFFFFFFF)
    val lightSurfaceVariant = Color(0xFFF1F3F5)
    val onLightAccent = Color(0xFF000000)
    val onDarkAccent = Color(0xFFFFFFFF)

    // Theme previews
    val previewLight = Color(0xFFFFFFFF)
    val previewBlack = Color(0xFF000000)
    val previewDark = Color(0xFF17202B)
    val previewAdaptive = Color(0xFF6B7A89)
    val previewBorder = Color(0xFF888888)
    val previewSelectedSwatch = Color(0xFFFFFFFF)
    val textOnLight = Color(0xFF000000)
    val textOnDark = Color(0xFFFFFFFF)

    // Top bar and dividers
    val topBarDividerTitle = Color(0xFFD5DBE2)
    val divider = Color(0xFF26303C)
    val swatchBorder = Color(0xFF3A4654)

    // Media rows
    val thumbnailBackground = Color(0xFF2B3744)
    val thumbnailIcon = Color(0xFF6B7A89)
    val lengthBadge = Color(0xCC000000)
    val lengthBadgeText = Color(0xFFFFFFFF)
    val tagText = Color(0xFFFFFFFF)
    val tagNew = Color(0xFFE53935)
    val tagSubtitle = Color(0xFF2EBD59)
    val folderGlyph = Color(0xFF444C55)

    // View menu
    val menuBackground = Color(0xFF26374C)
    val menuDivider = Color(0xFF39495C)
    val menuSegmentBorder = Color(0xFF3D5068)
    val menuText = Color(0xFFDADDE4)
    val menuCancel = Color(0xFF85929C)
    val menuCheckBorder = Color(0xFFB8C0CA)
    val menuCheckMark = Color(0xFFFFFFFF)

    // Music
    val trackIconBackground = Color(0xFFE4DCFA)
    val trackIcon = Color(0xFF9C7BEA)

    // Me page
    val mePage = Color(0xFF161A1E)
    val whatsApp = Color(0xFF25D366)

    val transparent = Color(0x00000000)
}

/** Accent colours offered on the App Theme screen. */
val accentSwatches = listOf(
    0xFFE0E0E0, 0xFF3D8BF2, 0xFF00BFA5, 0xFF43A047, 0xFFFFB300, 0xFFFB8C00, 0xFFE53935,
    0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF039BE5, 0xFF6D4C41,
    0xFF546E7A, 0xFF7CB342, 0xFFC0CA33, 0xFFF4511E, 0xFF00897B, 0xFF1E88E5,
)
