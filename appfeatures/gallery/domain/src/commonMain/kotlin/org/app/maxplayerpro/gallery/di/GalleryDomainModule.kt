package org.app.maxplayerpro.gallery.di

import org.app.maxplayerpro.gallery.ui.GalleryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun galleryDomainModule() = module {
    viewModel { GalleryViewModel(get(), get()) }
}
