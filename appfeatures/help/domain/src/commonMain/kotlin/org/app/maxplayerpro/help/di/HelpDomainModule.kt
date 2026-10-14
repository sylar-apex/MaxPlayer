package org.app.maxplayerpro.help.di

import org.app.maxplayerpro.help.ui.HelpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun helpDomainModule() = module {
    viewModel { HelpViewModel() }
}
