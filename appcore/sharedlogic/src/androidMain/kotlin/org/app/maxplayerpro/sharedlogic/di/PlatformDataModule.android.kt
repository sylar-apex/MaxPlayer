package org.app.maxplayerpro.sharedlogic.di

import android.content.Context
import org.app.maxplayerpro.sharedlogic.data.AndroidMediaRepository
import org.app.maxplayerpro.sharedlogic.data.AndroidPrivateFolder
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.PrefsStore
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { val context = get<Context>(); StoreFactory { name -> PrefsStore(context.getSharedPreferences(name, Context.MODE_PRIVATE)) } }
    single<MediaRepository> { AndroidMediaRepository(get(), get(), get()) }
    single<PrivateFolderRepository> { AndroidPrivateFolder(get()) }
}
