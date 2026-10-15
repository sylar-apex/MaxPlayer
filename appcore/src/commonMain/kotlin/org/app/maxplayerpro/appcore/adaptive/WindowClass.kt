package org.app.maxplayerpro.appcore.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

/** Width buckets the layouts adapt to: phones, tablets / small windows, laptops and desktops. */
enum class WindowClass {
    Compact, Medium, Expanded;

    val isWide: Boolean get() = this == Expanded

    companion object {
        fun of(width: Dp): WindowClass = when {
            width < 600.dp -> Compact
            width < 960.dp -> Medium
            else -> Expanded
        }
    }
}

val LocalWindowClass = staticCompositionLocalOf { WindowClass.Compact }

/** Measures the available width once and provides [LocalWindowClass] to everything inside. */
@Composable
fun ProvideWindowClass(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(LocalWindowClass provides WindowClass.of(maxWidth)) { content() }
    }
}

/**
 * Lets content extend [horizontal] beyond its parent's padding on both sides, e.g. a full-width
 * header placed inside a grid that pads its cells.
 */
fun androidx.compose.ui.Modifier.bleed(horizontal: Dp): androidx.compose.ui.Modifier =
    this.then(androidx.compose.ui.Modifier.layout { measurable, constraints ->
        val extra = horizontal.roundToPx() * 2
        val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    })
