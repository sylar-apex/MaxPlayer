package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.maxplayerpro.sharedlogic.data.FileRoot
import org.app.maxplayerpro.sharedlogic.data.PlaceKind
import org.app.maxplayerpro.sharedlogic.data.PlaceSection

/** Finder-style places as tiles under "Favourites" and "Locations"; a tap opens the folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlacesGrid(places: List<FileRoot>, onOpen: (FileRoot) -> Unit, modifier: Modifier = Modifier, tileWidth: Dp = 168.dp) {
    Column(modifier) {
        PlaceSection.entries.forEach { section ->
            val items = places.filter { it.section == section }
            if (items.isEmpty()) return@forEach
            Text(section.title.uppercase(), Modifier.padding(top = 12.dp, bottom = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { p ->
                    Row(
                        Modifier.width(tileWidth).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clickable { onOpen(p) }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(iconOf(p.kind), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(p.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun iconOf(kind: PlaceKind): ImageVector = when (kind) {
    PlaceKind.Home -> Icons.Default.Home
    PlaceKind.Disk -> Icons.Default.Storage
    PlaceKind.Removable -> Icons.Default.SdStorage
    PlaceKind.Desktop -> Icons.Default.DesktopMac
    PlaceKind.Documents -> Icons.Default.Description
    PlaceKind.Downloads -> Icons.Default.Download
    PlaceKind.Movies -> Icons.Default.Movie
    PlaceKind.Music -> Icons.Default.MusicNote
    PlaceKind.Pictures -> Icons.Default.Image
    PlaceKind.Camera -> Icons.Default.CameraAlt
    PlaceKind.Applications -> Icons.Default.Apps
}
