package org.app.maxplayerpro.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.maxplayerpro.appcore.component.FileList
import org.app.maxplayerpro.appcore.component.TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(onBack: () -> Unit, viewModel: SearchViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(state, onBack, onEvent = viewModel::setEvent)
}

@Composable
fun SearchContent(state: SearchContract.State, onBack: () -> Unit, onEvent: (SearchContract.Event) -> Unit) {
    var query by rememberSaveable { mutableStateOf(state.query) }
    fun change(q: String) { query = q; onEvent(SearchContract.Event.QueryChanged(q)) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Search", onBack = onBack)
        OutlinedTextField(query, ::change, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search videos") }, singleLine = true,
            keyboardActions = KeyboardActions(onSearch = { onEvent(SearchContract.Event.Submit(query)) }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
        if (query.isBlank()) {
            if (state.history.isNotEmpty()) {
                LazyColumn {
                    item { Text("Recent searches", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(state.history, key = { it }) { q ->
                        Text(q, Modifier.fillMaxWidth().clickable { change(q) }.padding(horizontal = 16.dp, vertical = 10.dp))
                    }
                }
            }
        } else FileList(state.results, state.options, "No results for \"$query\".")
    }
}
