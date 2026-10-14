package org.app.maxplayerpro.sharedlogic.data

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
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.model.PrivateFile
import java.io.File
import java.security.MessageDigest

/**
 * Files locked away in app-private storage, invisible to other apps and the media library,
 * protected by a PIN.
 */
class AndroidPrivateFolder(private val context: Context) : PrivateFolderRepository {
    private val dir = File(context.filesDir, "private").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("private", Context.MODE_PRIVATE)
    private val _files = MutableStateFlow(list())
    override val files: StateFlow<List<PrivateFile>> = _files.asStateFlow()

    override val hasPin: Boolean get() = prefs.contains(PIN)
    override fun setPin(pin: String) = prefs.edit().putString(PIN, hash(pin)).apply()
    override fun checkPin(pin: String) = prefs.getString(PIN, null) == hash(pin)

    private fun list() = dir.listFiles()?.sortedBy { it.name.lowercase() }.orEmpty().map { PrivateFile(it.path, it.name, it.length()) }
    private fun hash(pin: String) = MessageDigest.getInstance("SHA-256").digest(("nx:$pin").toByteArray()).joinToString("") { "%02x".format(it) }

    override suspend fun lockIn(file: MediaFile): Boolean = withContext(Dispatchers.IO) {
        val target = uniqueTarget(file.name)
        val ok = runCatching {
            context.contentResolver.openInputStream(Uri.parse(file.uri))!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.isSuccess
        if (!ok) target.delete()
        _files.value = list()
        ok
    }

    /** Moves [file] back to public storage under Movies/MaxPlayerPro. */
    override suspend fun restore(file: PrivateFile): Boolean = withContext(Dispatchers.IO) {
        val src = File(file.path)
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, src.name)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/MaxPlayerPro")
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
                context.contentResolver.openOutputStream(uri)!!.use { out -> src.inputStream().use { it.copyTo(out) } }
            } else {
                @Suppress("DEPRECATION")
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MaxPlayerPro/${src.name}")
                dest.parentFile?.mkdirs()
                src.copyTo(dest, overwrite = false)
            }
        }.isSuccess
        if (ok) src.delete()
        _files.value = list()
        ok
    }

    override fun delete(file: PrivateFile) { File(file.path).delete(); _files.value = list() }

    override fun rename(file: PrivateFile, name: String): Boolean = File(file.path).renameTo(File(dir, name)).also { _files.value = list() }

    private fun uniqueTarget(name: String): File {
        var f = File(dir, name)
        var i = 1
        while (f.exists()) f = File(dir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.')}").also { i++ }
        return f
    }

    private companion object { const val PIN = "pin" }
}
