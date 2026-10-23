package org.app.maxplayerpro.sharedlogic.platform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.android.KMediaPlayerAndroid
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

private val keys = AtomicInteger()

private suspend fun <I, O> launchForResult(contract: ActivityResultContract<I, O>, input: I): O? = withContext(Dispatchers.Main) {
    val activity = ActivityTracker.activity ?: return@withContext null
    suspendCancellableCoroutine { cont ->
        lateinit var launcher: ActivityResultLauncher<I>
        launcher = activity.activityResultRegistry.register("app_${keys.incrementAndGet()}", contract) { result ->
            launcher.unregister()
            if (cont.isActive) cont.resume(result)
        }
        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(input)
    }
}

actual object PlatformApp {
    private val context get() = KMediaPlayerAndroid.context

    actual suspend fun saveText(suggestedName: String, text: String): Boolean? {
        val uri: Uri = launchForResult(ActivityResultContracts.CreateDocument("text/plain"), suggestedName) ?: return null
        return withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }.isSuccess }
    }

    actual suspend fun openText(mimeTypes: List<String>): String? {
        val uri: Uri = launchForResult(ActivityResultContracts.OpenDocument(), mimeTypes.toTypedArray()) ?: return null
        return withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull() }
    }

    actual suspend fun pickFolder(): Pair<String, String>? {
        val tree: Uri = launchForResult(ActivityResultContracts.OpenDocumentTree(), null) ?: return null
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        // Keep the grant, so the same folder works for later downloads after a restart.
        runCatching { ActivityTracker.activity?.contentResolver?.takePersistableUriPermission(tree, flags) }
        val label = android.provider.DocumentsContract.getTreeDocumentId(tree).substringAfter(':').ifEmpty { "Selected folder" }
        return tree.toString() to label
    }

    actual fun openLanguageSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 33)
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
        else Intent(Settings.ACTION_LOCALE_SETTINGS)
        runCatching { (ActivityTracker.activity ?: return).startActivity(intent) }
    }

    actual fun quit() {
        ActivityTracker.activity?.finishAndRemoveTask()
    }
}
