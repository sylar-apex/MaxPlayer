package org.app.nxplayer.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.app.nxplayer.core.MediaFile
import java.io.File
import java.security.MessageDigest

/**
 * Files locked away in app-private storage, invisible to other apps and the media library,
 * protected by a PIN.
 */
class PrivateFolder(private val context: Context) {
    private val dir = File(context.filesDir, "private").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("private", Context.MODE_PRIVATE)
    private val _files = MutableStateFlow(list())
    val files: StateFlow<List<File>> = _files.asStateFlow()

    val hasPin: Boolean get() = prefs.contains(PIN)
    fun setPin(pin: String) = prefs.edit().putString(PIN, hash(pin)).apply()
    fun checkPin(pin: String) = prefs.getString(PIN, null) == hash(pin)

    private fun list() = dir.listFiles()?.sortedBy { it.name.lowercase() }.orEmpty()
    private fun hash(pin: String) = MessageDigest.getInstance("SHA-256").digest(("nx:$pin").toByteArray()).joinToString("") { "%02x".format(it) }

    /** Copies [file] into the private folder. The caller removes the original afterwards. */
    suspend fun lockIn(file: MediaFile): Boolean = withContext(Dispatchers.IO) {
        val target = uniqueTarget(file.name)
        val ok = runCatching {
            context.contentResolver.openInputStream(Uri.parse(file.uri))!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.isSuccess
        if (!ok) target.delete()
        _files.value = list()
        ok
    }

    /** Moves [file] back to public storage under Movies/NxPlayer. */
    suspend fun restore(file: File): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NxPlayer")
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
                context.contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            } else {
                @Suppress("DEPRECATION")
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "NxPlayer/${file.name}")
                dest.parentFile?.mkdirs()
                file.copyTo(dest, overwrite = false)
            }
        }.isSuccess
        if (ok) file.delete()
        _files.value = list()
        ok
    }

    fun delete(file: File) { file.delete(); _files.value = list() }

    fun rename(file: File, name: String): Boolean = file.renameTo(File(dir, name)).also { _files.value = list() }

    private fun uniqueTarget(name: String): File {
        var f = File(dir, name)
        var i = 1
        while (f.exists()) f = File(dir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.')}").also { i++ }
        return f
    }

    private companion object { const val PIN = "pin" }
}
