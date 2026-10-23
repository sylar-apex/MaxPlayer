package org.app.maxplayerpro.sharedlogic.di

import org.app.kmediaplayer.settings.KeyValueStore
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.MemoryPrivateFolder
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.data.WebMediaRepository
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module

@JsFun("(k) => window.localStorage.getItem(k)")
private external fun lsGet(key: String): String?

@JsFun("(k, v) => window.localStorage.setItem(k, v)")
private external fun lsSet(key: String, value: String)

@JsFun("(k) => window.localStorage.removeItem(k)")
private external fun lsRemove(key: String)

private class LocalStorageStore(private val prefix: String) : KeyValueStore {
    private val known = mutableSetOf<String>()
    override fun getString(key: String): String? = lsGet("$prefix.$key")
    override fun putString(key: String, value: String?) {
        if (value == null) { lsRemove("$prefix.$key"); known -= key } else { lsSet("$prefix.$key", value); known += key }
    }
    override fun keys(): Set<String> = known.toSet()
}

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { StoreFactory { name -> LocalStorageStore(name) } }
    single { WebMediaRepository() }
    single<MediaRepository> { get<WebMediaRepository>() }
    single<FolderImport> { get<WebMediaRepository>() }
    single<org.app.maxplayerpro.sharedlogic.data.PhotoRepository> { org.app.maxplayerpro.sharedlogic.data.NoPhotos() }
    single<org.app.maxplayerpro.sharedlogic.data.FileBrowser> { org.app.maxplayerpro.sharedlogic.data.NoFileBrowser }
    single<PrivateFolderRepository> { MemoryPrivateFolder() }
}
