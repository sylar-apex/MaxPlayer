package org.app.maxplayerpro.folder.di

import org.app.maxplayerpro.folder.ui.FolderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun folderDomainModule() = module {
    viewModel { (path: String) -> FolderViewModel(path, get(), get(), get(), get()) }
}
