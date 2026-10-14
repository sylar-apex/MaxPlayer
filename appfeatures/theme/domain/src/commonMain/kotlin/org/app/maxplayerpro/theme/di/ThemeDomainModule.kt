package org.app.maxplayerpro.theme.di

import org.app.maxplayerpro.theme.ui.ThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun themeDomainModule() = module {
    viewModel { ThemeViewModel(get()) }
}
