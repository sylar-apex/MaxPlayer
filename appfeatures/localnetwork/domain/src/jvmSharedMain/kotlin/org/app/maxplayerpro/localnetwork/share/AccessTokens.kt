package org.app.maxplayerpro.localnetwork.share

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Stateless access tokens: `payload.signature`, where the payload names what is opened (the device, or one share),
 * the session epoch and an expiry, and the signature is HMAC-SHA256 with this device's secret. Nothing is kept in
 * memory, so tokens survive restarts; raising the epoch ("log out all devices") voids every older token at once.
 */
internal class AccessTokens(secret: String) {
    private val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    private val b64 = Base64.getUrlEncoder().withoutPadding()
    private val b64d = Base64.getUrlDecoder()

    fun issue(scope: String, epoch: Int, expiresAtMs: Long): String {
        val payload = b64.encodeToString("$scope|$epoch|$expiresAtMs".toByteArray())
        return "$payload.${sign(payload)}"
    }

    /** True when [token] was issued here for [scope] in [epoch] and has not expired. */
    fun valid(token: String?, scope: String, epoch: Int, nowMs: Long): Boolean = read(token, epoch, nowMs) == scope

    /** The scope [token] was issued for, when it is genuine, from [epoch] and not expired; else null. */
    fun read(token: String?, epoch: Int, nowMs: Long): String? {
        val parts = token?.split('.') ?: return null
        if (parts.size != 2 || !MessageDigest.isEqual(sign(parts[0]).toByteArray(), parts[1].toByteArray())) return null
        val fields = runCatching { String(b64d.decode(parts[0])).split('|') }.getOrNull() ?: return null
        if (fields.size != 3) return null
        return fields[0].takeIf { fields[1].toIntOrNull() == epoch && (fields[2].toLongOrNull() ?: 0) > nowMs }
    }

    /** True when [token] carries this server's signature, whatever its epoch or expiry: it was real once. */
    fun genuine(token: String?): Boolean {
        val parts = token?.split('.') ?: return false
        return parts.size == 2 && MessageDigest.isEqual(sign(parts[0]).toByteArray(), parts[1].toByteArray())
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return b64.encodeToString(mac.doFinal(payload.toByteArray()))
    }

    companion object {
        /** A signed-in device; [clientId] lets the owner log that one device out. */
        fun device(clientId: String) = "device:$clientId"
        /** One protected share opened by [clientId]. */
        fun share(id: String, clientId: String) = "share:$id:$clientId"
        fun clientOf(scope: String): String = scope.substringAfterLast(':')
    }
}
