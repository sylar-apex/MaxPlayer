package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_back
import org.jetbrains.compose.resources.painterResource

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, divider: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) IconButton(onBack, Modifier.width(56.dp)) { Icon(painterResource(Res.drawable.ic_back), "Navigate up") }
            else Spacer(Modifier.width(16.dp))
            if (onBack != null) Spacer(Modifier.width(4.dp))
            Text(title, Modifier.weight(1f), fontSize = if (divider) 18.sp else 17.5.sp, fontWeight = if (divider) FontWeight.Medium else FontWeight.SemiBold,
                color = if (divider) MaxColors.topBarDividerTitle else Color.Unspecified, maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
        }
        if (divider) HorizontalDivider(color = MaxColors.divider, thickness = 1.dp)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(text, color = MaxColors.tagText, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp))
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}

/** A titled page explaining why a feature is not available. */
@Composable
fun InfoScreen(title: String, reason: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(title, onBack = onBack)
        Text(reason, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}
