package org.app.maxplayerpro.music.di

import org.app.maxplayerpro.music.ui.MusicViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun musicDomainModule() = module {
    viewModel { MusicViewModel(get(), get(), get()) }
}
