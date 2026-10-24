package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.settings.KeyValueStore
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Device-wide sharing settings. [sessionEpoch] goes up on "log out all devices"; tokens from older epochs stop working. */
@Serializable
data class DeviceSharing(
    val pin: String? = null,
    val untilMs: Long? = null,
    val sessionEpoch: Int = 0,
    /** Devices signed in to this one, by their device id; removing one logs that device out. */
    val sessions: Map<String, ViewerSession> = emptyMap(),
)

/** Another device that opened this one: shown in "Log out devices". */
@Serializable
data class ViewerSession(val name: String, val platform: String, val signedInAtMs: Long)

/** PINs this device entered on another device, kept as the tokens they returned until that device logs us out. */
@Serializable
data class RememberedAccess(val deviceToken: String? = null, val shareTokens: Map<String, String> = emptyMap())

/**
 * The shares this device offers and its sharing settings, saved across restarts. Changes run one at a time under
 * [lock] and replace whole immutable values, so the server always reads a consistent snapshot without locking and
 * the saved copy never falls behind the one in memory.
 */
class ShareStore(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val _shares = MutableStateFlow(decode(KEY_SHARES) ?: emptyList<Share>())
    val shares: StateFlow<List<Share>> = _shares.asStateFlow()
    private val _device = MutableStateFlow(decode(KEY_DEVICE) ?: DeviceSharing())
    val device: StateFlow<DeviceSharing> = _device.asStateFlow()
    private val _remembered = MutableStateFlow(decode(KEY_REMEMBERED) ?: emptyMap<String, RememberedAccess>())
    /** Keyed by the other device's id. */
    val remembered: StateFlow<Map<String, RememberedAccess>> = _remembered.asStateFlow()

    /** This device's stable id, so others can tell it apart across address changes. */
    val deviceId: String = store.getString(KEY_DEVICE_ID) ?: newId().also { store.putString(KEY_DEVICE_ID, it) }

    /** Signs access tokens; never leaves this device. */
    val tokenSecret: String = store.getString(KEY_SECRET) ?: (newId() + newId()).also { store.putString(KEY_SECRET, it) }

    var hostingEnabled: Boolean
        get() = store.getString(KEY_HOSTING) == "1"
        set(value) = store.putString(KEY_HOSTING, if (value) "1" else null)

    /** Addresses the user typed in, for devices discovery cannot see (other subnets, the browser build). */
    var savedAddresses: List<String>
        get() = decode<List<String>>(KEY_SAVED) ?: emptyList()
        set(value) = store.putString(KEY_SAVED, json.encodeToString(value.distinct()))

    /**
     * Shares [files] (one file, or a folder's files when [folderPath] is set). Sharing the same folder or single file
     * again replaces its share, keeping the id so devices that already unlocked it keep their access.
     */
    suspend fun save(
        title: String,
        files: List<MediaFile>,
        folderPath: String?,
        access: ShareAccess,
        pin: String?,
        nowMs: Long,
        startsAtMs: Long?,
        expiresAtMs: Long?,
        allowDownload: Boolean = false,
    ): Share {
        var saved: Share? = null
        mutate { list ->
            val existing = list.firstOrNull { it.matches(files, folderPath) }
            val share = Share(
                id = existing?.id ?: newId(),
                title = title.ifBlank { files.firstOrNull()?.title ?: "Shared files" },
                items = files.mapIndexed { i, f -> f.toItem("$i", folderPath) },
                access = access,
                code = if (access == ShareAccess.Protected) pin?.filter { it.isLetterOrDigit() }?.ifEmpty { null } ?: newCode() else null,
                createdAtMs = existing?.createdAtMs ?: nowMs,
                startsAtMs = startsAtMs,
                expiresAtMs = expiresAtMs,
                folderPath = folderPath,
                allowDownload = allowDownload,
            )
            saved = share
            if (existing == null) list + share else list.map { if (it.id == existing.id) share else it }
        }
        return saved!!
    }

    /** The share currently covering [files] / [folderPath], if any; used to show and edit its mode. */
    fun findFor(files: List<MediaFile>, folderPath: String?): Share? = _shares.value.firstOrNull { it.matches(files, folderPath) }

    suspend fun revoke(id: String) = mutate { list -> list.map { if (it.id == id) it.copy(revoked = true) else it } }

    /** Stops sharing the file [uri] wherever it is shared; a share left with no files goes away. */
    suspend fun removeFile(uri: String) = mutate { list ->
        list.mapNotNull { share ->
            if (share.items.none { it.uri == uri }) share
            else share.copy(items = share.items.filterNot { it.uri == uri }).takeIf { it.items.isNotEmpty() }
        }
    }

    /**
     * Stops sharing the subfolder [path] of a shared folder (everything under it); the shared folder itself when
     * [path] is one. A share left with no files goes away.
     */
    suspend fun removeUnder(path: String) = mutate { list ->
        list.mapNotNull { share ->
            when {
                share.folderPath == path -> null
                share.items.none { it.path.startsWith("$path/") } -> share
                else -> share.copy(items = share.items.filterNot { it.path.startsWith("$path/") }).takeIf { it.items.isNotEmpty() }
            }
        }
    }

    /** Adds [files] to the shared folder they are in (the innermost one); files already in it are skipped. */
    suspend fun addToFolder(files: List<MediaFile>) = mutate { list ->
        val owner = list.filter { s -> s.folderPath != null && files.all { it.path.startsWith(s.folderPath + "/") } }.maxByOrNull { it.folderPath!!.length }
            ?: return@mutate list
        val known = owner.items.map { it.uri }.toSet()
        var next = (owner.items.maxOfOrNull { it.id.toIntOrNull() ?: -1 } ?: -1) + 1
        val added = files.filter { it.uri !in known }.map { it.toItem("${next++}", owner.folderPath) }
        list.map { if (it.id == owner.id) it.copy(items = it.items + added) else it }
    }

    /** Makes the share private again. */
    suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun find(id: String): Share? = _shares.value.firstOrNull { it.id == id }

    suspend fun setDevicePin(pin: String?) = lock.withLock {
        // A new or removed PIN logs out every device that used the old one.
        val d = _device.value
        writeDevice(d.copy(pin = pin?.filter { it.isLetterOrDigit() }?.ifEmpty { null }, sessionEpoch = d.sessionEpoch + 1, sessions = emptyMap()))
    }

    suspend fun setDeviceUntil(untilMs: Long?) = lock.withLock { writeDevice(_device.value.copy(untilMs = untilMs)) }

    suspend fun logOutAllDevices() = lock.withLock {
        writeDevice(_device.value.copy(sessionEpoch = _device.value.sessionEpoch + 1, sessions = emptyMap()))
    }

    suspend fun logOutDevice(clientId: String) = lock.withLock { writeDevice(_device.value.copy(sessions = _device.value.sessions - clientId)) }

    suspend fun signIn(clientId: String, session: ViewerSession) = lock.withLock {
        val d = _device.value
        if (d.sessions[clientId]?.name != session.name) writeDevice(d.copy(sessions = d.sessions + (clientId to session)))
    }

    suspend fun remember(deviceId: String, change: (RememberedAccess) -> RememberedAccess?) = lock.withLock {
        val current = _remembered.value
        val next = change(current[deviceId] ?: RememberedAccess())
        val map = if (next == null || (next.deviceToken == null && next.shareTokens.isEmpty())) current - deviceId else current + (deviceId to next)
        store.putString(KEY_REMEMBERED, json.encodeToString(map))
        _remembered.value = map
    }

    private fun writeDevice(next: DeviceSharing) {
        store.putString(KEY_DEVICE, json.encodeToString(next))
        _device.value = next
    }

    private suspend fun mutate(change: (List<Share>) -> List<Share>) = lock.withLock {
        val next = change(_shares.value)
        store.putString(KEY_SHARES, json.encodeToString(next))
        _shares.value = next
    }

    private fun Share.matches(files: List<MediaFile>, folder: String?): Boolean =
        if (folder != null) folderPath == folder else folderPath == null && items.size == 1 && files.size == 1 && items[0].uri == files[0].uri

    private fun MediaFile.toItem(id: String, folderPath: String?): SharedItem {
        val dir = folderPath?.let { path.removePrefix("$it/").substringBeforeLast('/', "") } ?: ""
        return SharedItem(id, uri, path, name, sizeBytes, durationMs, mimeType.ifEmpty { guessMime(name, isAudio) }, isAudio, width, height, dir)
    }

    private inline fun <reified T> decode(key: String): T? =
        store.getString(key)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    companion object {
        private const val KEY_SHARES = "shares"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_HOSTING = "hosting"
        private const val KEY_SAVED = "savedAddresses"
        private const val KEY_DEVICE = "deviceSharing"
        private const val KEY_SECRET = "tokenSecret"
        private const val KEY_REMEMBERED = "remembered"

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
