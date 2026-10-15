package org.app.maxplayerpro.sharedlogic.scan

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

/** Root folders of every mounted storage volume: internal storage, SD cards and USB drives. */
object StorageVolumes {
    fun directories(context: Context): List<File> {
        val manager = context.getSystemService(StorageManager::class.java)
        val mounted = manager.storageVolumes.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
        val dirs = if (Build.VERSION.SDK_INT >= 30) mounted.mapNotNull { it.directory }
        else listOf(Environment.getExternalStorageDirectory()) + context.getExternalFilesDirs(null).drop(1).mapNotNull { it?.volumeRoot() }
        return dirs.filter { it.canRead() }.distinctBy { it.path }
    }

    /** `/storage/XXXX-XXXX/Android/data/<pkg>/files` → `/storage/XXXX-XXXX`. */
    private fun File.volumeRoot(): File? = path.substringBefore("/Android/", "").takeIf { it.isNotEmpty() }?.let(::File)
}
