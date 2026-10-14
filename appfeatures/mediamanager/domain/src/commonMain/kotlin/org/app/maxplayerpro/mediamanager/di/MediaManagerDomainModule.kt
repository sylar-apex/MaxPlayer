package org.app.maxplayerpro.mediamanager.di

import org.app.maxplayerpro.mediamanager.domain.model.MediaListKind
import org.app.maxplayerpro.mediamanager.ui.MediaListViewModel
import org.app.maxplayerpro.mediamanager.ui.MediaManagerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun mediaManagerDomainModule() = module {
    viewModel { MediaManagerViewModel(get(), get(), get(), get()) }
    viewModel { (kind: MediaListKind) -> MediaListViewModel(kind, get(), get(), get(), get()) }
}
