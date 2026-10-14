package org.app.maxplayerpro.sharedlogic.platform

import android.content.Intent
import org.app.kmediaplayer.android.KMediaPlayerAndroid

/** Extra carrying an [AppScreen.key] on the main activity's intent. */
const val EXTRA_OPEN_SCREEN = "open"

actual fun openAppScreen(screen: AppScreen) {
    val context = KMediaPlayerAndroid.context
    val target = KMediaPlayerAndroid.musicActivity ?: return
    context.startActivity(Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_OPEN_SCREEN, screen.key))
}
