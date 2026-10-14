package org.app.maxplayerpro.search.di

import org.app.maxplayerpro.search.ui.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun searchDomainModule() = module {
    viewModel { SearchViewModel(get(), get(), get(), get()) }
}
