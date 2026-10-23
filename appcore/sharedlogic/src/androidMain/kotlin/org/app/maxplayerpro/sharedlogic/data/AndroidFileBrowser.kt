package org.app.maxplayerpro.sharedlogic.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import org.app.maxplayerpro.sharedlogic.scan.StorageVolumes

/** Internal storage and SD cards; full browsing needs "All files access" (Android 11+). */
class AndroidFileBrowser(private val context: Context) : JvmFileBrowser() {
    override val hasAccess: Boolean get() = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    override fun requestAccess() {
        if (Build.VERSION.SDK_INT < 30) return
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    override fun roots(): List<FileRoot> = StorageVolumes.directories(context).mapIndexed { i, dir ->
        FileRoot(if (i == 0) "Internal storage" else "SD card ${if (i > 1) i else ""}".trim(), dir.path)
    }.ifEmpty { listOf(FileRoot("Internal storage", Environment.getExternalStorageDirectory().path)) }
}
