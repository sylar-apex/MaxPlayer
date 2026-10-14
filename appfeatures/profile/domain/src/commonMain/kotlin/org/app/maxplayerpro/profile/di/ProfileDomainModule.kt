package org.app.maxplayerpro.profile.di

import org.app.maxplayerpro.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun profileDomainModule() = module {
    viewModel { ProfileViewModel(get()) }
}
