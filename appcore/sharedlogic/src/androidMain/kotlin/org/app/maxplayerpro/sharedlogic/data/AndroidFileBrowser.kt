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

    override fun roots(): List<FileRoot> {
        val volumes = StorageVolumes.directories(context).ifEmpty { listOf(Environment.getExternalStorageDirectory()) }
        val internal = volumes.first()
        val favourites = listOf(
            "Download" to PlaceKind.Downloads, "DCIM" to PlaceKind.Camera, "Pictures" to PlaceKind.Pictures,
            "Movies" to PlaceKind.Movies, "Music" to PlaceKind.Music, "Documents" to PlaceKind.Documents,
        ).map { (name, kind) -> java.io.File(internal, name) to kind }.filter { (f, _) -> f.isDirectory }
            .map { (f, kind) -> FileRoot(if (f.name == "DCIM") "Camera" else f.name, f.path, kind, PlaceSection.Favourites) }
        val locations = volumes.mapIndexed { i, dir ->
            FileRoot(if (i == 0) "Internal storage" else "SD card ${if (i > 1) i else ""}".trim(), dir.path, if (i == 0) PlaceKind.Disk else PlaceKind.Removable)
        }
        return favourites + locations
    }
}
