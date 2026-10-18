package org.app.maxplayerpro.appcore.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** True when the app draws into the window title bar (macOS transparent title bar), so pages put their header there. */
val LocalInTitleBar = staticCompositionLocalOf { false }

/** Wraps title-bar content so dragging it moves the window (desktop); plain content elsewhere. */
val LocalTitleBarArea = staticCompositionLocalOf<@Composable (@Composable () -> Unit) -> Unit> { { it() } }

/** Height of the title bar, including its divider; the shell pads the rest of the window by this much. */
val TitleBarHeight: Dp = 52.dp

/** What the current page shows in the title bar: set by [WideHeader] while the shell draws a [TitleBar]. */
@Stable
class TitleBarHost {
    internal var page by mutableStateOf<TitleBarPage?>(null)

    /** Forgets the previous page's title and actions, e.g. on navigation to a page without a [WideHeader]. */
    fun clear() { page = null }
}

internal class TitleBarPage(
    val title: String,
    val subtitle: String?,
    val onBack: (() -> Unit)?,
    val onSearch: (() -> Unit)?,
    val actions: @Composable RowScope.() -> Unit,
)

/** Non-null while the shell draws a [TitleBar]: [WideHeader] then hands its content to it instead of drawing in the page. */
val LocalTitleBarHost = staticCompositionLocalOf<TitleBarHost?> { null }

@Composable
internal fun PublishToTitleBar(host: TitleBarHost, page: TitleBarPage) {
    SideEffect { host.page = page }
}

/**
 * macOS title bar across the whole window, beside the traffic lights: the page title, then back, search and
 * refresh centred, the page's actions (grid / list) at the end. The divider below doubles as a progress line
 * while [refreshing].
 */
@Composable
fun TitleBar(host: TitleBarHost, refreshing: Boolean, onRefresh: () -> Unit) {
    val page = host.page
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().height(TitleBarHeight).background(colors.surface)) {
        Box(Modifier.fillMaxWidth().weight(1f).padding(start = 84.dp, end = 12.dp)) {
            if (page != null) Column(Modifier.align(Alignment.CenterStart).widthIn(max = 260.dp)) {
                Text(page.title, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (page.subtitle != null) Text(page.subtitle, fontSize = 11.sp, lineHeight = 13.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.align(Alignment.Center).widthIn(max = 560.dp), verticalAlignment = Alignment.CenterVertically) {
                BarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", page?.onBack)
                SearchPill(Modifier.padding(horizontal = 8.dp).weight(1f), page?.onSearch)
                BarButton(Icons.Filled.Refresh, "Refresh", onRefresh.takeUnless { refreshing })
            }
            if (page != null) Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, content = page.actions)
        }
        // Divider; while refreshing a light track with the progress running along it.
        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = colors.primary, trackColor = colors.onSurface.copy(alpha = 0.08f), strokeCap = StrokeCap.Butt, gapSize = 0.dp)
        else Box(Modifier.fillMaxWidth().height(1.dp).background(colors.onSurface.copy(alpha = 0.08f)))
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(18.dp), tint = if (onClick != null) colors.onSurface else colors.onSurface.copy(alpha = 0.3f))
    }
}

/** Looks like a search field; opens the search page. */
@Composable
private fun SearchPill(modifier: Modifier, onClick: (() -> Unit)?) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.height(30.dp).clip(shape).background(colors.surfaceVariant)
            .border(1.dp, if (hovered && onClick != null) colors.primary else colors.onSurface.copy(alpha = 0.08f), shape)
            .hoverable(source).clickable(enabled = onClick != null, interactionSource = source, indication = null) { onClick?.invoke() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(15.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("Search videos", fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1)
    }
}
