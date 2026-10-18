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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
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

    /** What is typed in the title bar's search box; the search page shows its results. */
    var query by mutableStateOf("")

    /** Counts Enter presses in the search box, so the search page can save the query to history. */
    var submits by mutableStateOf(0)
        private set

    internal fun submit() { if (query.isNotBlank()) submits++ }

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
 * macOS title bar across the whole window, beside the traffic lights: the page title, then back, forward, a search box and
 * refresh centred, the page's actions (grid / list) at the end. The divider below doubles as a progress line
 * while [refreshing].
 */
@Composable
fun TitleBar(
    host: TitleBarHost,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    /** Browser-style history; null when there is nothing to go back / forward to. */
    onBack: (() -> Unit)?,
    onForward: (() -> Unit)?,
    /** Typing in the search box: the shell opens the search page if it is not showing. */
    onSearchTyped: () -> Unit,
) {
    val page = host.page
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().height(TitleBarHeight).background(colors.surface)) {
        Box(Modifier.fillMaxWidth().weight(1f).padding(start = 84.dp, end = 12.dp)) {
            if (page != null) Column(Modifier.align(Alignment.CenterStart).widthIn(max = 260.dp)) {
                Text(page.title, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (page.subtitle != null) Text(page.subtitle, fontSize = 11.sp, lineHeight = 13.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.align(Alignment.Center).widthIn(max = 560.dp), verticalAlignment = Alignment.CenterVertically) {
                BarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                BarButton(Icons.AutoMirrored.Filled.ArrowForward, "Forward", onForward)
                SearchField(Modifier.padding(horizontal = 8.dp).weight(1f), host, onSearchTyped)
                BarButton(Icons.Filled.Refresh, "Refresh", onRefresh.takeUnless { refreshing })
            }
            if (page != null) Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, content = page.actions)
        }
        // Divider; while refreshing a faint track with a soft progress line running along it (never bright on dark themes).
        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = colors.onSurface.copy(alpha = 0.28f), trackColor = colors.onSurface.copy(alpha = 0.08f), strokeCap = StrokeCap.Butt, gapSize = 0.dp)
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

/** The title bar's search box: typing searches as you go, Enter saves the search to history. */
@Composable
private fun SearchField(modifier: Modifier, host: TitleBarHost, onType: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val focused = remember { MutableInteractionSource() }
    val isFocused by focused.collectIsFocusedAsState()
    BasicTextField(
        value = host.query,
        onValueChange = { host.query = it; if (it.isNotBlank()) onType() },
        singleLine = true,
        interactionSource = focused,
        textStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { host.submit() }),
        modifier = modifier.height(30.dp).onPreviewKeyEvent { e ->
            when {
                e.type != KeyEventType.KeyDown -> false
                e.key == Key.Enter -> { host.submit(); true }
                e.key == Key.Escape && host.query.isNotEmpty() -> { host.query = ""; true }
                else -> false
            }
        },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(shape).background(colors.surfaceVariant)
                    .border(1.dp, if (isFocused) colors.onSurface.copy(alpha = 0.3f) else colors.onSurface.copy(alpha = 0.08f), shape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(15.dp), tint = colors.onSurfaceVariant)
                // Hint and typed text share one box, so the cursor starts exactly where the hint does.
                Box(Modifier.padding(start = 8.dp).weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (host.query.isEmpty()) Text("Search videos", fontSize = 12.sp, lineHeight = 16.sp, color = colors.onSurfaceVariant, maxLines = 1)
                    inner()
                }
                if (host.query.isNotEmpty()) Icon(Icons.Default.Close, "Clear", Modifier.size(15.dp).clip(RoundedCornerShape(50)).clickable { host.query = "" }, tint = colors.onSurfaceVariant)
            }
        },
    )
}
