package org.app.maxplayerpro.settings.di

import org.app.maxplayerpro.settings.ui.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun settingsDomainModule() = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
