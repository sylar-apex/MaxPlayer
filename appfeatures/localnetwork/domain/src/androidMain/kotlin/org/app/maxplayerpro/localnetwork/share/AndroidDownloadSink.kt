package org.app.maxplayerpro.localnetwork.share

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * Saves into the shared Downloads folder (through MediaStore, which needs no storage permission on Android 10+),
 * or a folder the user picked with the system folder picker (a granted document tree).
 */
internal class AndroidDownloadSink(private val context: Context) : DownloadSink {
    override val defaultLabel = "Downloads"

    override fun create(name: String, mimeType: String, location: SaveLocation): DownloadFile = when (location) {
        is SaveLocation.Folder -> inTree(Uri.parse(location.id), location.label, name, mimeType)
        SaveLocation.Default -> if (Build.VERSION.SDK_INT >= 29) inMediaStore(name, mimeType) else inLegacyDownloads(name)
    }

    private fun inMediaStore(name: String, mimeType: String): DownloadFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType.ifEmpty { "application/octet-stream" })
            // Hidden from other apps until complete.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Downloads folder unavailable")
        val pfd = resolver.openFileDescriptor(uri, "rw") ?: error("Can't write to Downloads")
        val channel = FileOutputStream(pfd.fileDescriptor).channel
        return object : DownloadFile {
            override val channel: FileChannel = channel
            override fun finish(): String {
                runCatching { pfd.close() }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                return "Downloads/$name"
            }
            override fun abort() { runCatching { pfd.close() }; runCatching { resolver.delete(uri, null, null) } }
        }
    }

    private fun inTree(tree: Uri, label: String, name: String, mimeType: String): DownloadFile {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val doc = DocumentsContract.createDocument(resolver, parent, mimeType.ifEmpty { "application/octet-stream" }, name) ?: error("Can't create the file there")
        val pfd = resolver.openFileDescriptor(doc, "rw") ?: error("Can't write there")
        val channel = FileOutputStream(pfd.fileDescriptor).channel
        return object : DownloadFile {
            override val channel: FileChannel = channel
            override fun finish(): String { runCatching { pfd.close() }; return "$label/$name" }
            override fun abort() { runCatching { pfd.close() }; runCatching { DocumentsContract.deleteDocument(resolver, doc) } }
        }
    }

    @Suppress("DEPRECATION")
    private fun inLegacyDownloads(name: String): DownloadFile {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
        var target = File(dir, name)
        var i = 1
        while (target.exists()) target = File(dir, "${name.substringBeforeLast('.', name)} (${i++})${name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }}")
        val channel = FileOutputStream(target).channel
        return object : DownloadFile {
            override val channel: FileChannel = channel
            override fun finish() = target.absolutePath
            override fun abort() { target.delete() }
        }
    }
}
