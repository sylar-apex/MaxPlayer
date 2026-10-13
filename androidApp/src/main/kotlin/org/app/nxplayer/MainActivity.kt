package org.app.nxplayer

import android.Manifest
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
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.ui.AppRoot
import org.app.nxplayer.ui.NxTheme

class MainActivity : ComponentActivity() {
    private val permissions = if (Build.VERSION.SDK_INT >= 33)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) request.launch(missing.toTypedArray()) else refresh()
        setContent { NxTheme { AppRoot(intent.getStringExtra("open")) } }
    }

    override fun onResume() {
        super.onResume()
        if (AppGraph.settings[org.app.nxplayer.core.Keys.autoRefresh]) refresh()
    }

    private fun refresh() { lifecycleScope.launch { AppGraph.media.refresh() } }
}
