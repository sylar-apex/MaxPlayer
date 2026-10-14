package org.app.maxplayerpro.privatefolder.di

import org.app.maxplayerpro.privatefolder.ui.PrivateFolderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun privateFolderDomainModule() = module {
    viewModel { PrivateFolderViewModel(get(), get(), get()) }
}
