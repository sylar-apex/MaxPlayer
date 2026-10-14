package org.app.maxplayerpro.localnetwork.di

import org.app.maxplayerpro.localnetwork.ui.LocalNetworkViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun localNetworkDomainModule() = module {
    viewModel { LocalNetworkViewModel(get()) }
}
