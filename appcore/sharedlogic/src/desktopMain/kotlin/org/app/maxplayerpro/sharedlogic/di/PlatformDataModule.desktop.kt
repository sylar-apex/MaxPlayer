package org.app.maxplayerpro.sharedlogic.di

import org.app.kmediaplayer.settings.KeyValueStore
import org.app.maxplayerpro.sharedlogic.data.DesktopMediaRepository
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.MemoryPrivateFolder
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

private class PreferencesStore(private val node: Preferences) : KeyValueStore {
    override fun getString(key: String): String? = node.get(key, null)
    override fun putString(key: String, value: String?) { if (value == null) node.remove(key) else node.put(key, value) }
    override fun keys(): Set<String> = node.keys().toSet()
}

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { StoreFactory { name -> PreferencesStore(Preferences.userRoot().node("org/app/maxplayerpro/$name")) } }
    single<FolderImport> { FolderImport.None }
    single<MediaRepository> { DesktopMediaRepository() }
    single<PrivateFolderRepository> { MemoryPrivateFolder() }
}
