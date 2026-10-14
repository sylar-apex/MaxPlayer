package org.app.maxplayerpro.di

import com.psbapp.di.initKoin
import org.app.maxplayerpro.player.di.configureMediaPlayer
import org.koin.mp.KoinPlatform

private var started = false

/** Starts Koin and hands the app's settings and player host to the media library. For desktop, iOS and web. */
fun initApp() {
    if (started) return
    started = true
    initKoin(AppModules)
    KoinPlatform.getKoin().configureMediaPlayer()
}
