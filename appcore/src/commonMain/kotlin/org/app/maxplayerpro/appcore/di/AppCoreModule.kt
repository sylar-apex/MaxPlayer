package org.app.maxplayerpro.appcore.di

import org.app.maxplayerpro.appcore.component.ThumbnailLoader
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache
import org.koin.dsl.module

fun appCoreModule() = module {
    single<ThumbnailCache> { ThumbnailLoader }
}
