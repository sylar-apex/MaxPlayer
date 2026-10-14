package org.app.maxplayerpro.sharedlogic.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Screens other parts of the app (the player, the notification) can ask the main window to open. */
enum class AppScreen(val key: String) {
    Settings("settings"), Help("help");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } }
}

/** Requests for the main window's navigation, collected by the app's root navigation host. */
object AppScreenRequests {
    private val _requests = MutableSharedFlow<AppScreen>(extraBufferCapacity = 4)
    val requests: SharedFlow<AppScreen> = _requests.asSharedFlow()

    fun emit(screen: AppScreen) { _requests.tryEmit(screen) }
}

/** Brings the main window forward and shows [screen]. */
expect fun openAppScreen(screen: AppScreen)
