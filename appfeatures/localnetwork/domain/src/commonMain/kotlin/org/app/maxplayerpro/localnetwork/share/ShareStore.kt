package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.settings.KeyValueStore
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The shares this device offers, saved across restarts. Changes run one at a time under [lock] and replace the
 * whole immutable list, so the server always reads a consistent snapshot without locking and the saved copy
 * never falls behind the one in memory.
 */
class ShareStore(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val _shares = MutableStateFlow(load())
    val shares: StateFlow<List<Share>> = _shares.asStateFlow()

    /** This device's stable id, so others can tell it apart across address changes. */
    val deviceId: String = store.getString(KEY_DEVICE_ID) ?: newId().also { store.putString(KEY_DEVICE_ID, it) }

    var hostingEnabled: Boolean
        get() = store.getString(KEY_HOSTING) == "1"
        set(value) = store.putString(KEY_HOSTING, if (value) "1" else null)

    /** Addresses the user typed in, for devices discovery cannot see (other subnets, the browser build). */
    var savedAddresses: List<String>
        get() = store.getString(KEY_SAVED)?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
        set(value) = store.putString(KEY_SAVED, json.encodeToString(value.distinct()))

    suspend fun create(
        title: String,
        files: List<MediaFile>,
        access: ShareAccess,
        code: String?,
        nowMs: Long,
        startsAtMs: Long?,
        expiresAtMs: Long?,
    ): Share {
        val share = Share(
            id = newId(),
            title = title.ifBlank { files.firstOrNull()?.title ?: "Shared files" },
            items = files.mapIndexed { i, f ->
                SharedItem("$i", f.uri, f.path, f.name, f.sizeBytes, f.durationMs, f.mimeType.ifEmpty { guessMime(f.name, f.isAudio) }, f.isAudio, f.width, f.height)
            },
            access = access,
            code = if (access == ShareAccess.Code) code?.filter { it.isLetterOrDigit() }?.ifEmpty { null } ?: newCode() else null,
            createdAtMs = nowMs,
            startsAtMs = startsAtMs,
            expiresAtMs = expiresAtMs,
        )
        mutate { it + share }
        return share
    }

    suspend fun revoke(id: String) = mutate { list -> list.map { if (it.id == id) it.copy(revoked = true) else it } }

    suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun find(id: String): Share? = _shares.value.firstOrNull { it.id == id }

    private suspend fun mutate(change: (List<Share>) -> List<Share>) = lock.withLock {
        val next = change(_shares.value)
        store.putString(KEY_SHARES, json.encodeToString(next))
        _shares.value = next
    }

    private fun load(): List<Share> =
        store.getString(KEY_SHARES)?.let { runCatching { json.decodeFromString<List<Share>>(it) }.getOrNull() } ?: emptyList()

    companion object {
        private const val KEY_SHARES = "shares"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_HOSTING = "hosting"
        private const val KEY_SAVED = "savedAddresses"

        /** 6 digits from a cryptographically secure source. */
        @OptIn(ExperimentalUuidApi::class)
        fun newCode(): String {
            val bytes = Uuid.random().toByteArray()
            val n = bytes.take(4).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
            return (n % 1_000_000).toString().padStart(6, '0')
        }

        @OptIn(ExperimentalUuidApi::class)
        fun newId(): String = Uuid.random().toHexString()
    }
}

fun guessMime(name: String, isAudio: Boolean): String = when (name.substringAfterLast('.', "").lowercase()) {
    "mp4", "m4v" -> "video/mp4"
    "mkv" -> "video/x-matroska"
    "webm" -> "video/webm"
    "mov" -> "video/quicktime"
    "avi" -> "video/x-msvideo"
    "ts", "m2ts" -> "video/mp2t"
    "mp3" -> "audio/mpeg"
    "m4a", "aac" -> "audio/mp4"
    "flac" -> "audio/flac"
    "ogg", "opus" -> "audio/ogg"
    "wav" -> "audio/wav"
    else -> if (isAudio) "audio/*" else "video/*"
}
