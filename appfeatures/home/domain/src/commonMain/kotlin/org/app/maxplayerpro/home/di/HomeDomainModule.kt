package org.app.maxplayerpro.home.di

import org.app.maxplayerpro.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun homeDomainModule() = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
}
