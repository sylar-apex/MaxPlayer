package org.app.maxplayerpro.sharedlogic.platform

actual fun openAppScreen(screen: AppScreen) = AppScreenRequests.emit(screen)
