package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_arrow_down
import org.app.maxplayerpro.appres.resources.ic_arrow_up
import org.app.maxplayerpro.appres.resources.ic_check
import org.app.maxplayerpro.appres.resources.ic_chevron_down
import org.app.maxplayerpro.appres.resources.ic_lay_grid
import org.app.maxplayerpro.appres.resources.ic_lay_list
import org.app.maxplayerpro.appres.resources.ic_sort_date
import org.app.maxplayerpro.appres.resources.ic_sort_framerate
import org.app.maxplayerpro.appres.resources.ic_sort_length
import org.app.maxplayerpro.appres.resources.ic_sort_path
import org.app.maxplayerpro.appres.resources.ic_sort_played
import org.app.maxplayerpro.appres.resources.ic_sort_resolution
import org.app.maxplayerpro.appres.resources.ic_sort_size
import org.app.maxplayerpro.appres.resources.ic_sort_status
import org.app.maxplayerpro.appres.resources.ic_sort_title
import org.app.maxplayerpro.appres.resources.ic_sort_type
import org.app.maxplayerpro.appres.resources.ic_vm_all
import org.app.maxplayerpro.appres.resources.ic_vm_files
import org.app.maxplayerpro.appres.resources.ic_vm_folders
import org.app.maxplayerpro.sharedlogic.model.ListField
import org.app.maxplayerpro.sharedlogic.model.ListLayout
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.SortKey
import org.app.maxplayerpro.sharedlogic.model.ViewMode
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** View mode, layout, sort, visible fields and advanced list options. [onDone] receives the edited options. */
@Composable
fun ViewMenuDialog(current: ListViewOptions, onDone: (ListViewOptions) -> Unit, onDismiss: () -> Unit) {
    var o by remember { mutableStateOf(current) }
    var fieldsOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(299.dp).height(517.dp).background(MaxColors.menuBackground, RoundedCornerShape(3.dp))) {
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Column(Modifier.width(167.dp)) {
                        MenuTitle("View Mode")
                        Row {
                            ViewMode.entries.forEachIndexed { i, m ->
                                MenuChoice(listOf(Res.drawable.ic_vm_all, Res.drawable.ic_vm_files, Res.drawable.ic_vm_folders)[i], m.label, m == o.viewMode, Modifier.width(56.dp)) { o = o.copy(viewMode = m) }
                            }
                        }
                    }
                    Box(Modifier.padding(top = 54.dp).size(width = 1.dp, height = 45.dp).background(MaxColors.menuDivider))
                    Column(Modifier.width(107.dp)) {
                        MenuTitle("Layout")
                        Row {
                            ListLayout.entries.forEachIndexed { i, l ->
                                MenuChoice(listOf(Res.drawable.ic_lay_list, Res.drawable.ic_lay_grid)[i], l.label, l == o.layout, Modifier.width(54.dp)) { o = o.copy(layout = l) }
                            }
                        }
                    }
                }
                MenuDividerLine(Modifier.padding(top = 8.dp))
                Column(Modifier.padding(horizontal = 12.dp)) {
                    MenuTitle("Sort", bottom = 12.dp)
                    val sortIcons = listOf(
                        Res.drawable.ic_sort_title, Res.drawable.ic_sort_date, Res.drawable.ic_sort_played, Res.drawable.ic_sort_status, Res.drawable.ic_sort_length,
                        Res.drawable.ic_sort_size, Res.drawable.ic_sort_resolution, Res.drawable.ic_sort_path, Res.drawable.ic_sort_framerate, Res.drawable.ic_sort_type,
                    )
                    SortKey.entries.chunked(5).forEachIndexed { r, row ->
                        Row(Modifier.padding(top = if (r == 0) 0.dp else 1.dp)) {
                            row.forEachIndexed { c, k -> MenuChoice(sortIcons[r * 5 + c], k.label, k == o.sortKey, Modifier.width(55.dp)) { o = o.copy(sortKey = k) } }
                        }
                    }
                    Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp).size(width = 243.dp, height = 28.dp).border(1.dp, MaxColors.menuSegmentBorder, RoundedCornerShape(2.dp))) {
                        SortDirection(Res.drawable.ic_arrow_up, "A to Z", o.ascending, accent, Modifier.weight(1f)) { o = o.copy(ascending = true) }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(MaxColors.menuSegmentBorder))
                        SortDirection(Res.drawable.ic_arrow_down, "Z to A", !o.ascending, accent, Modifier.weight(1f)) { o = o.copy(ascending = false) }
                    }
                }
                MenuDividerLine()
                MenuExpander("Fields", fieldsOpen) { fieldsOpen = !fieldsOpen }
                if (fieldsOpen) Column(Modifier.padding(start = 20.dp, end = 20.dp)) {
                    ListField.entries.chunked(3).forEach { row ->
                        Row { row.forEach { fld -> MenuCheck(fld.label, fld in o.fields, Modifier.width(86.5.dp)) { o = o.copy(fields = if (fld in o.fields) o.fields - fld else o.fields + fld) } } }
                    }
                }
                MenuDividerLine()
                MenuExpander("Advanced", advancedOpen) { advancedOpen = !advancedOpen }
                if (advancedOpen) Column(Modifier.padding(start = 20.dp, end = 20.dp)) {
                    MenuCheck("Display length over thumbnail", o.lengthOverThumbnail) { o = o.copy(lengthOverThumbnail = !o.lengthOverThumbnail) }
                    MenuCheck("Show hidden files and folders", o.showHidden) { o = o.copy(showHidden = !o.showHidden) }
                    MenuCheck("Recognize .nomedia", o.respectNoMedia) { o = o.copy(respectNoMedia = !o.respectNoMedia) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp, bottom = 9.dp), horizontalArrangement = Arrangement.End) {
                Text("Cancel", Modifier.clickable(onClick = onDismiss).height(39.dp).padding(horizontal = 12.dp).wrapContentHeight(), fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = MaxColors.menuCancel)
                Spacer(Modifier.width(4.dp))
                Text("Done", Modifier.clickable { onDone(o); onDismiss() }.height(39.dp).padding(horizontal = 12.dp).wrapContentHeight(),
                    fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
    }
}

@Composable
private fun MenuTitle(text: String, bottom: Dp = 13.dp) =
    Text(text, Modifier.padding(start = 12.dp, top = 12.dp, bottom = bottom), fontSize = 14.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = MaxColors.menuText)

@Composable
private fun MenuDividerLine(modifier: Modifier = Modifier) = Box(modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(MaxColors.menuDivider))

@Composable
private fun MenuChoice(icon: DrawableResource, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaxColors.menuText
    Column(modifier.height(60.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(icon), label, Modifier.size(24.dp), tint = color)
        Spacer(Modifier.height(6.dp))
        Text(label, Modifier.wrapContentWidth(unbounded = true), fontSize = 11.sp, lineHeight = 13.sp, color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun SortDirection(icon: DrawableResource, label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) accent else MaxColors.menuText
    Row(
        modifier.fillMaxHeight().let { if (selected) it.border(1.dp, accent, RoundedCornerShape(2.dp)).background(accent.copy(alpha = 0.08f)) else it }.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(12.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.5.sp, color = color)
    }
}

@Composable
private fun MenuExpander(title: String, open: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick).padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = MaxColors.menuText)
        Icon(painterResource(Res.drawable.ic_chevron_down), null, Modifier.size(16.dp).graphicsLayer { rotationZ = if (open) 180f else 0f }, tint = MaxColors.menuText)
    }
}

@Composable
private fun MenuCheck(label: String, checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Row(modifier.heightIn(min = 40.dp).clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp))
                .border(2.dp, if (checked) MaterialTheme.colorScheme.primary else MaxColors.menuCheckBorder, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(14.dp), tint = MaxColors.menuCheckMark) }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, lineHeight = 14.sp, color = MaxColors.menuText)
    }
}
