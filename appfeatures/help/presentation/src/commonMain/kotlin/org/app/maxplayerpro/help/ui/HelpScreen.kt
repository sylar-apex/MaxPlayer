package org.app.maxplayerpro.help.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.maxplayerpro.appcore.component.TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HelpScreen(onBack: () -> Unit, viewModel: HelpViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HelpContent(state, onBack)
}

@Composable
fun HelpContent(state: HelpContract.State, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Help", onBack = onBack)
        LazyColumn {
            items(state.topics, key = { it.question }) { t ->
                Column {
                    Text(t.question, Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(t.answer, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
