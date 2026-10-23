package org.app.maxplayerpro.files.di

import org.app.maxplayerpro.files.ui.FilesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun filesDomainModule() = module {
    viewModel { FilesViewModel(get(), get()) }
}
