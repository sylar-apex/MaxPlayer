package org.app.maxplayerpro.player.di

import org.app.kmediaplayer.KMediaPlayer
import org.app.maxplayerpro.player.data.MaxPlayerHost
import org.koin.core.Koin
import org.koin.dsl.module

fun playerDomainModule() = module {
    single { MaxPlayerHost(get(), get()) }
}

/** Hands the app's settings store and [MaxPlayerHost] to the media player library. Call once after Koin starts. */
fun Koin.configureMediaPlayer() {
    KMediaPlayer.configure(settings = get(), host = get<MaxPlayerHost>())
}
