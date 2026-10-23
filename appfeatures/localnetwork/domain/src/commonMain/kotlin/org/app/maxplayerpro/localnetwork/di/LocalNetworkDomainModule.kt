package org.app.maxplayerpro.localnetwork.di

import org.app.maxplayerpro.localnetwork.share.ShareStore
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.localnetwork.share.platformShareModule
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkViewModel
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun localNetworkDomainModule() = module {
    includes(platformShareModule())
    single { ShareStore(get<StoreFactory>().open("share")) }
    single { SharingController(get(), get(), get()) }
    viewModel { LocalNetworkViewModel(get(), get(), get()) }
}
