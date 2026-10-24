package org.app.maxplayerpro.localnetwork.share

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who may open a share. Public: anyone who can open this device. Protected: only with the share's own PIN.
 * "Private" is simply not being shared, so it has no value here.
 */
@Serializable
enum class ShareAccess {
    Public,
    // Saved as "Code" by the first version of sharing.
    @SerialName("Code") Protected,
}

/** A file offered by a share. [id] is stable within the share and is what streaming URLs name. */
@Serializable
data class SharedItem(
    val id: String,
    val uri: String,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val mimeType: String,
    val isAudio: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    /** Subfolder inside a shared folder, like `React Js/src`; empty for files at its top. */
    val dir: String = "",
)

/**
 * Files this device offers on the network: one file, or the files of [folderPath] as they were when shared (files
 * added to the folder later stay private until it is shared again). Open only between [startsAtMs] and
 * [expiresAtMs] (null for "now" / "never") and until revoked; [code] is the PIN of a [ShareAccess.Protected] share.
 */
@Serializable
data class Share(
    val id: String,
    val title: String,
    val items: List<SharedItem>,
    val access: ShareAccess,
    val code: String? = null,
    val createdAtMs: Long,
    val startsAtMs: Long? = null,
    val expiresAtMs: Long? = null,
    val revoked: Boolean = false,
    val folderPath: String? = null,
    /** Others may save the files to their device (download button and `/download` links). */
    val allowDownload: Boolean = false,
) {
    val isFolder: Boolean get() = folderPath != null

    fun isOpen(nowMs: Long): Boolean =
        !revoked && (startsAtMs == null || nowMs >= startsAtMs) && (expiresAtMs == null || nowMs < expiresAtMs)

    fun isExpired(nowMs: Long): Boolean = expiresAtMs != null && nowMs >= expiresAtMs
}

// ---- Wire format between Max Player devices (and the web page). Version the path, not these classes. ----

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
    val platform: String,
    val port: Int,
    val protocol: Int = PROTOCOL_VERSION,
    /** The owner set a device PIN: everything except this endpoint needs a device token from `login`. */
    val pinRequired: Boolean = false,
)

/** Signs a device in; [clientId] names the device so the owner can see it and log it out on its own. */
@Serializable
data class LoginRequest(val pin: String, val clientId: String = "", val clientName: String = "", val clientPlatform: String = "")

/** A device token, valid until the owner logs everyone out or changes the device PIN. */
@Serializable
data class LoginResponse(val token: String)

/** A share as listed to other devices. [items] is empty while it is protected and not yet unlocked. */
@Serializable
data class RemoteShare(
    val id: String,
    val title: String,
    val locked: Boolean,
    val itemCount: Int,
    val expiresAtMs: Long? = null,
    val items: List<RemoteItem> = emptyList(),
    val folder: Boolean = false,
    /** Every file is audio; lets a viewer file a locked share under Music before its items are visible. */
    val audio: Boolean = false,
    /** The owner allows saving these files; items then carry a [RemoteItem.downloadPath]. */
    val downloadable: Boolean = false,
    /** What the files are: [KIND_VIDEO], [KIND_AUDIO], [KIND_IMAGE] or [KIND_FILE] (anything else, or a mix with other files). */
    val kind: String = KIND_VIDEO,
)

const val KIND_VIDEO = "video"
const val KIND_AUDIO = "audio"
const val KIND_IMAGE = "image"
const val KIND_FILE = "file"

/** The kind of a set of files, from their MIME types. */
fun kindOf(mimeTypes: List<String>, allAudio: Boolean): String = when {
    mimeTypes.isEmpty() -> KIND_VIDEO
    allAudio || mimeTypes.all { it.startsWith("audio/") } -> KIND_AUDIO
    mimeTypes.all { it.startsWith("image/") } -> KIND_IMAGE
    mimeTypes.all { it.startsWith("video/") || it.isEmpty() } -> KIND_VIDEO
    else -> KIND_FILE
}

/** A playable item; [path] is relative to the host and carries no access token. */
@Serializable
data class RemoteItem(
    val id: String, val name: String, val sizeBytes: Long, val durationMs: Long, val mimeType: String, val isAudio: Boolean, val path: String,
    /** Frame size, so the player picks portrait or landscape before the first frame arrives; 0 when unknown. */
    val width: Int = 0,
    val height: Int = 0,
    /** Path that sends the file as a download; null when the owner doesn't allow downloads. */
    val downloadPath: String? = null,
    /** Subfolder inside the shared folder (`a/b`), so the viewer can show the same tree; empty at the top. */
    val dir: String = "",
    /** A small JPEG of a photo, for lists and grids; null for other files. */
    val thumbPath: String? = null,
)

@Serializable
data class UnlockRequest(val code: String)

/** Returned for a correct share PIN: [token] opens the share's items until [expiresAtMs]. */
@Serializable
data class UnlockResponse(val token: String, val expiresAtMs: Long, val share: RemoteShare)

/** Header carrying the device token on API calls; media URLs carry it as the `d` query parameter instead. */
const val DEVICE_TOKEN_HEADER = "X-Device-Token"
/** Header carrying a share token on API calls; media URLs carry it as `t`. */
const val SHARE_TOKEN_HEADER = "X-Share-Token"
/** Response header telling which PIN is missing on a 401: [AUTH_DEVICE] or [AUTH_SHARE]. */
const val AUTH_HEADER = "X-Auth-Needed"
const val AUTH_DEVICE = "device"
/** Sent with a device 401 when the token was real but the owner has logged that device out. */
const val AUTH_REASON_HEADER = "X-Auth-Reason"
const val AUTH_REASON_LOGGED_OUT = "logged-out"
const val AUTH_SHARE = "share"

const val PROTOCOL_VERSION = 2
const val DEFAULT_SHARE_PORT = 53319
