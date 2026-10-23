package org.app.maxplayerpro.localnetwork.share

import kotlinx.serialization.Serializable

/** Who may open a share: anyone on the network, or only someone who enters its code. */
@Serializable
enum class ShareAccess { Public, Code }

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
)

/**
 * Files this device offers on the network. The share is open only between [startsAtMs] and [expiresAtMs]
 * (either may be null for "now" / "never") and until it is revoked; [code] is set when [access] is [ShareAccess.Code].
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
) {
    fun isOpen(nowMs: Long): Boolean =
        !revoked && (startsAtMs == null || nowMs >= startsAtMs) && (expiresAtMs == null || nowMs < expiresAtMs)

    fun isExpired(nowMs: Long): Boolean = expiresAtMs != null && nowMs >= expiresAtMs
}

// ---- Wire format between Max Player devices (and the web page). Version the path, not these classes. ----

@Serializable
data class DeviceInfo(val id: String, val name: String, val platform: String, val port: Int, val protocol: Int = PROTOCOL_VERSION)

/** A share as listed to other devices. [items] is empty while it is locked behind a code. */
@Serializable
data class RemoteShare(
    val id: String,
    val title: String,
    val locked: Boolean,
    val itemCount: Int,
    val expiresAtMs: Long? = null,
    val items: List<RemoteItem> = emptyList(),
)

/** A playable item; [path] is relative to the host and carries no access token. */
@Serializable
data class RemoteItem(
    val id: String, val name: String, val sizeBytes: Long, val durationMs: Long, val mimeType: String, val isAudio: Boolean, val path: String,
    /** Frame size, so the player picks portrait or landscape before the first frame arrives; 0 when unknown. */
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class UnlockRequest(val code: String)

/** Returned for a correct code: [token] opens the share's items until [expiresAtMs]. */
@Serializable
data class UnlockResponse(val token: String, val expiresAtMs: Long, val share: RemoteShare)

const val PROTOCOL_VERSION = 1
const val DEFAULT_SHARE_PORT = 53319
