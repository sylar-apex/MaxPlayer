package org.app.maxplayerpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.app.kmediaplayer.KMediaPlayer
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.platform.AppScreen
import org.app.maxplayerpro.sharedlogic.platform.AppScreenRequests
import org.app.maxplayerpro.sharedlogic.platform.EXTRA_OPEN_SCREEN
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
    private val permissions = if (Build.VERSION.SDK_INT >= 33)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    private val media: MediaRepository get() = KoinPlatform.getKoin().get()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) request.launch(missing.toTypedArray()) else refresh()
        val start = AppScreen.of(intent.getStringExtra(EXTRA_OPEN_SCREEN))
        setContent { App(startScreen = start) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppScreen.of(intent.getStringExtra(EXTRA_OPEN_SCREEN))?.let(AppScreenRequests::emit)
    }

    override fun onResume() {
        super.onResume()
        if (KMediaPlayer.settings[AppKeys.autoRefresh]) refresh()
    }

    private fun refresh() { lifecycleScope.launch { media.refresh() } }
}
