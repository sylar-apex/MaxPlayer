package org.app.maxplayerpro.gallery.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.app.maxplayerpro.sharedlogic.data.PhotoRepository
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareRequests

/** Albums (photo folders, newest first) for the Gallery; scans on first open. */
class GalleryViewModel(private val repo: PhotoRepository, private val operations: MediaOperations) :
    MVIBaseViewModel<GalleryContract.Event, GalleryContract.State, GalleryContract.Effect>() {

    override fun createInitialState() = GalleryContract.State()
    override fun handleEffect(effect: GalleryContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(repo.photos, repo.loading) { photos, loading -> photos to loading }.collect { (photos, loading) ->
                val albums = photos.groupBy { it.folderPath }.map { (p, list) -> GalleryContract.Album(p, list) }
                    .sortedByDescending { a -> a.photos.maxOf { it.dateModifiedMs } }
                updateState { copy(albums = albums, loading = loading) }
            }
        }
        if (repo.photos.value.isEmpty()) viewModelScope.launch { repo.refresh() }
    }

    override fun handleEvent(event: GalleryContract.Event) {
        when (event) {
            GalleryContract.Event.Refresh -> viewModelScope.launch { repo.refresh() }
            is GalleryContract.Event.Share -> NetworkShareRequests.request(event.photos, event.folderPath)
            is GalleryContract.Event.Delete -> viewModelScope.launch { operations.delete(event.photos); repo.refresh() }
        }
    }
}
