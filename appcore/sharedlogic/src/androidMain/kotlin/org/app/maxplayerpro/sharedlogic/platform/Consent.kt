package org.app.maxplayerpro.sharedlogic.platform

import android.app.Activity
import android.app.Application
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/** Tracks the foreground activity so system consent dialogs can be shown from any screen. */
object ActivityTracker : Application.ActivityLifecycleCallbacks {
    private var current: WeakReference<ComponentActivity>? = null

    val activity: ComponentActivity? get() = current?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    fun install(application: Application) = application.registerActivityLifecycleCallbacks(this)

    override fun onActivityResumed(activity: Activity) { if (activity is ComponentActivity) current = WeakReference(activity) }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { if (activity is ComponentActivity) current = WeakReference(activity) }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (current?.get() === activity) current = null }
}

private val consentKeys = AtomicInteger()

/** Shows the MediaStore consent dialog behind [sender]; true if the user allowed it. */
suspend fun requestConsent(sender: IntentSender): Boolean = withContext(Dispatchers.Main) {
    val activity = ActivityTracker.activity ?: return@withContext false
    suspendCancellableCoroutine { cont ->
        lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
        launcher = activity.activityResultRegistry.register("consent_${consentKeys.incrementAndGet()}", ActivityResultContracts.StartIntentSenderForResult()) { result ->
            launcher.unregister()
            if (cont.isActive) cont.resume(result.resultCode == Activity.RESULT_OK)
        }
        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    }
}
