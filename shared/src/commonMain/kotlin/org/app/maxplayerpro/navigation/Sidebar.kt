package org.app.maxplayerpro.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.psbapp.navigation.utils.push
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.reflect.KClass

/** One sidebar destination; [routes] are the screens that keep it highlighted. */
private class SidebarItem(val label: String, val icon: ImageVector, val route: Any, val routes: List<KClass<*>>)

private val library = listOf(
    SidebarItem("Videos", Icons.Outlined.VideoLibrary, Routes.Home, listOf(Routes.Home::class, Routes.Folder::class, Routes.Search::class)),
    SidebarItem("Music", Icons.Outlined.MusicNote, Routes.Music, listOf(Routes.Music::class)),
    SidebarItem("Playlists", Icons.AutoMirrored.Outlined.PlaylistPlay, Routes.Playlists, listOf(Routes.Playlists::class, Routes.PlaylistDetail::class)),
    SidebarItem("Private folder", Icons.Outlined.Lock, Routes.PrivateFolder, listOf(Routes.PrivateFolder::class)),
)

private val more = listOf(
    SidebarItem("Network share", Icons.Outlined.SettingsInputAntenna, Routes.LocalNetwork, listOf(Routes.LocalNetwork::class)),
    SidebarItem("Storage", Icons.Outlined.Storage, Routes.MediaManager, listOf(Routes.MediaManager::class, Routes.MediaList::class)),
    SidebarItem("Theme", Icons.Outlined.Palette, Routes.Theme, listOf(Routes.Theme::class)),
)

private val footer = listOf(
    SidebarItem("Settings", Icons.Outlined.Settings, Routes.Settings, listOf(Routes.Settings::class, Routes.SettingsPage::class, Routes.Tuner::class)),
    SidebarItem("Help", Icons.AutoMirrored.Outlined.HelpOutline, Routes.Help, listOf(Routes.Help::class)),
)

/** Desktop and wide-window navigation: brand, library sections and settings, always visible. */
@Composable
internal fun Sidebar(navController: NavController, modifier: Modifier = Modifier) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    fun open(item: SidebarItem) {
        if (item.route == Routes.Home) { navController.popBackStack(Routes.Home, inclusive = false); return }
        // Sidebar destinations sit directly on Home, so switching sections never piles up history.
        navController.popBackStack(Routes.Home, inclusive = false)
        navController.push(item.route)
    }
    val border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Column(
        modifier.width(248.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)
            // Hairline on the right edge separating the sidebar from the content.
            .drawBehind { drawLine(border, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1.dp.toPx()) }
            .padding(horizontal = 14.dp),
    ) {
        // Compact brand row: one line, with a small PRO chip.
        Row(Modifier.padding(start = 10.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.app_logo), null, Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)))
            Spacer(Modifier.width(10.dp))
            Text("Max Player", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(8.dp))
            Text(
                "PRO", fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Section("LIBRARY")
            library.forEach { NavRow(it, destination.isIn(it)) { open(it) } }
            Section("MORE")
            more.forEach { NavRow(it, destination.isIn(it)) { open(it) } }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        Column(Modifier.padding(vertical = 10.dp)) { footer.forEach { NavRow(it, destination.isIn(it)) { open(it) } } }
    }
}

private fun NavDestination?.isIn(item: SidebarItem): Boolean = this != null && item.routes.any { hasRoute(it) }

@Composable
private fun Section(title: String) {
    Text(
        title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun NavRow(item: SidebarItem, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val accent = MaterialTheme.colorScheme.primary
    val background = when {
        selected -> accent.copy(alpha = 0.16f)
        hovered -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).background(background)
            .hoverable(source).clickable(interactionSource = source, indication = null, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, null, Modifier.size(20.dp), tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(
            item.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
