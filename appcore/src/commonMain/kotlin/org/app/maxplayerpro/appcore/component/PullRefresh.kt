package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Pull down to reload, the same on every list page; the spinner stays while [refreshing]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    PullToRefreshBox(refreshing, onRefresh, modifier, content = content)
}
