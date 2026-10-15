package org.app.maxplayerpro.sharedlogic.di

import org.app.kmediaplayer.settings.KeyValueStore
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.MemoryPrivateFolder
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.data.SimpleMediaRepository
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

private class UserDefaultsStore(private val prefix: String) : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun getString(key: String): String? = defaults.stringForKey("$prefix.$key")
    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey("$prefix.$key") else defaults.setObject(value, "$prefix.$key")
    }
    override fun keys(): Set<String> = defaults.dictionaryRepresentation().keys.mapNotNull { (it as? String)?.removePrefix("$prefix.")?.takeIf { k -> k != it } }.toSet()
}

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { StoreFactory { name -> UserDefaultsStore(name) } }
    single<FolderImport> { FolderImport.None }
    single<MediaRepository> { SimpleMediaRepository() }
    single<PrivateFolderRepository> { MemoryPrivateFolder() }
}
