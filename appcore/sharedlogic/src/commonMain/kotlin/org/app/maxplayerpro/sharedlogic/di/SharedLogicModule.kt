package org.app.maxplayerpro.sharedlogic.di

import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.settings.appSettingRegistries
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations
import org.app.maxplayerpro.sharedlogic.ui.filelist.FileListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** App-wide data: settings, user records, the media library, the private folder and shared file actions. */
fun sharedLogicModule() = module {
    includes(platformDataModule())
    single { ObservableSettings(get<StoreFactory>().open("settings"), appSettingRegistries) }
    single { UserDataRepository(get<StoreFactory>().open("user")) }
    factory { MediaOperations(get(), get(), get()) }
    viewModel { FileListViewModel(get(), get(), get(), get(), get()) }
}

/** Binds [StoreFactory], the media repository and the private folder for the current platform. */
expect fun platformDataModule(): Module
