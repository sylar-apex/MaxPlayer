package org.app.maxplayerpro.playlists.di

import org.app.maxplayerpro.playlists.ui.PlaylistDetailViewModel
import org.app.maxplayerpro.playlists.ui.PlaylistsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun playlistsDomainModule() = module {
    viewModel { PlaylistsViewModel(get()) }
    viewModel { (id: Long) -> PlaylistDetailViewModel(id, get(), get(), get()) }
}
