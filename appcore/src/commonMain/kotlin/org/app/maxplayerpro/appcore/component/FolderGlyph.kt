package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import org.app.maxplayerpro.appcore.theme.MaxColors

/** The library's folder shape, used by Home and by folders shared from other devices. */
@Composable
fun FolderGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = h * 0.09f
        val tabTop = w * 0.33f
        val tabEnd = w * 0.49f
        val bodyY = h * 0.145f
        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(tabTop, 0f)
            cubicTo(tabTop + (tabEnd - tabTop) * 0.45f, 0f, tabTop + (tabEnd - tabTop) * 0.55f, bodyY, tabEnd, bodyY)
            lineTo(w - r, bodyY)
            quadraticTo(w, bodyY, w, bodyY + r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        drawPath(path, MaxColors.folderGlyph)
    }
}

