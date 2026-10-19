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

/**
 * Java preferences cap each value at [Preferences.MAX_VALUE_LENGTH] (8192) characters, and user data (history,
 * folders, playlists) outgrows that. Longer values are split across `key#0`, `key#1`, … with the count stored
 * under `key` behind [CHUNKED], so callers still see one string.
 */
private class PreferencesStore(private val node: Preferences) : KeyValueStore {
    override fun getString(key: String): String? {
        val raw = node.get(key, null) ?: return null
        val count = raw.removePrefix(CHUNKED).takeIf { raw.startsWith(CHUNKED) }?.toIntOrNull() ?: return raw
        return buildString { repeat(count) { append(node.get(chunkKey(key, it), "")) } }
    }

    override fun putString(key: String, value: String?) {
        removeChunks(key)
        when {
            value == null -> node.remove(key)
            value.length <= Preferences.MAX_VALUE_LENGTH && !value.startsWith(CHUNKED) -> node.put(key, value)
            else -> {
                val parts = value.chunked(Preferences.MAX_VALUE_LENGTH)
                parts.forEachIndexed { i, part -> node.put(chunkKey(key, i), part) }
                node.put(key, CHUNKED + parts.size)
            }
        }
    }

    override fun keys(): Set<String> = node.keys().filterNot { CHUNK_SEPARATOR in it }.toSet()

    private fun removeChunks(key: String) {
        val raw = node.get(key, null) ?: return
        if (!raw.startsWith(CHUNKED)) return
        repeat(raw.removePrefix(CHUNKED).toIntOrNull() ?: 0) { node.remove(chunkKey(key, it)) }
    }

    private fun chunkKey(key: String, index: Int) = "$key$CHUNK_SEPARATOR$index"

    private companion object {
        const val CHUNKED = "\u0001chunks:"
        const val CHUNK_SEPARATOR = "#"
    }
}

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { StoreFactory { name -> PreferencesStore(Preferences.userRoot().node("org/app/maxplayerpro/$name")) } }
    single<FolderImport> { FolderImport.None }
    single<MediaRepository> { DesktopMediaRepository() }
    single<PrivateFolderRepository> { MemoryPrivateFolder() }
}
