package org.app.maxplayerpro.localnetwork.di

import org.app.maxplayerpro.localnetwork.share.ShareMarksSource
import org.app.maxplayerpro.localnetwork.share.ShareStore
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.localnetwork.share.platformShareModule
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkViewModel
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareMarks
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun localNetworkDomainModule() = module {
    includes(platformShareModule())
    single { ShareStore(get<StoreFactory>().open("share")) }
    single { SharingController(get(), get(), get(), get()) }
    single<NetworkShareMarks> { ShareMarksSource(get(), get()) }
    viewModel { LocalNetworkViewModel(get(), get(), get()) }
}
