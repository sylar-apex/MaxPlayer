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

    override fun create(name: String, mimeType: String, location: SaveLocation, subdir: String): DownloadFile = when (location) {
        is SaveLocation.Folder -> inTree(Uri.parse(location.id), location.label, name, mimeType, subdir)
        SaveLocation.Default -> if (Build.VERSION.SDK_INT >= 29) inMediaStore(name, mimeType, subdir) else inLegacyDownloads(name, subdir)
    }

    private fun inMediaStore(name: String, mimeType: String, subdir: String): DownloadFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType.ifEmpty { "application/octet-stream" })
            if (subdir.isNotEmpty()) put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + subdir)
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
                return listOf("Downloads", subdir, name).filter { it.isNotEmpty() }.joinToString("/")
            }
            override fun abort() { runCatching { pfd.close() }; runCatching { resolver.delete(uri, null, null) } }
        }
    }

    private fun inTree(tree: Uri, label: String, name: String, mimeType: String, subdir: String): DownloadFile {
        val resolver = context.contentResolver
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val parent = subdir.split('/').filter { it.isNotEmpty() }.fold(root) { dir, part -> childFolder(tree, dir, part) }
        val doc = DocumentsContract.createDocument(resolver, parent, mimeType.ifEmpty { "application/octet-stream" }, name) ?: error("Can't create the file there")
        val pfd = resolver.openFileDescriptor(doc, "rw") ?: error("Can't write there")
        val channel = FileOutputStream(pfd.fileDescriptor).channel
        return object : DownloadFile {
            override val channel: FileChannel = channel
            override fun finish(): String { runCatching { pfd.close() }; return listOf(label, subdir, name).filter { it.isNotEmpty() }.joinToString("/") }
            override fun abort() { runCatching { pfd.close() }; runCatching { DocumentsContract.deleteDocument(resolver, doc) } }
        }
    }

    /** The folder [name] inside [dir] of a document tree, created when missing. Synchronized: folder downloads reuse folders. */
    @Synchronized
    private fun childFolder(tree: Uri, dir: Uri, name: String): Uri {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir))
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        return DocumentsContract.createDocument(resolver, dir, DocumentsContract.Document.MIME_TYPE_DIR, name) ?: error("Can't create folder $name")
    }

    @Suppress("DEPRECATION")
    private fun inLegacyDownloads(name: String, subdir: String): DownloadFile {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).let { if (subdir.isEmpty()) it else File(it, subdir) }.apply { mkdirs() }
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
