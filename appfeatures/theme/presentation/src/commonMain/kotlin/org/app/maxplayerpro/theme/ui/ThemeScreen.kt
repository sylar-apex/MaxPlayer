package org.app.maxplayerpro.theme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appcore.theme.accentSwatches
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ThemeScreen(onBack: () -> Unit, viewModel: ThemeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ThemeContent(state, onBack, onEvent = viewModel::setEvent)
}

@Composable
fun ThemeContent(state: ThemeContract.State, onBack: () -> Unit, onEvent: (ThemeContract.Event) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("App Theme", onBack = onBack)
        state.modes.chunked(2).forEach { row ->
            Row(Modifier.padding(horizontal = 12.dp)) {
                row.forEach { (key, label) ->
                    val preview = when (key) { "light" -> MaxColors.previewLight; "black" -> MaxColors.previewBlack; "dark" -> MaxColors.previewDark; else -> MaxColors.previewAdaptive }
                    Box(
                        Modifier.weight(1f).padding(6.dp).height(80.dp).background(preview, RoundedCornerShape(10.dp))
                            .border(if (key == state.mode) 3.dp else 1.dp, if (key == state.mode) MaterialTheme.colorScheme.primary else MaxColors.previewBorder, RoundedCornerShape(10.dp))
                            .clickable { onEvent(ThemeContract.Event.SelectMode(key)) }.padding(12.dp),
                    ) {
                        Text(label, color = if (key == "light") MaxColors.textOnLight else MaxColors.textOnDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart))
                        if (key == state.mode) Text("Using", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
        Text("Classic Themes", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accentSwatches.forEach { c ->
                Box(Modifier.size(48.dp).background(Color(c), RoundedCornerShape(4.dp))
                    .border(if (c == state.accent) 2.dp else 0.dp, if (c == state.accent) MaxColors.previewSelectedSwatch else MaxColors.transparent, RoundedCornerShape(4.dp))
                    .clickable { onEvent(ThemeContract.Event.SelectAccent(c)) })
            }
        }
    }
}
