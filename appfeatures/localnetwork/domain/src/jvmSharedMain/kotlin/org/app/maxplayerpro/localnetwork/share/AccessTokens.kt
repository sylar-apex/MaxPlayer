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
    fun valid(token: String?, scope: String, epoch: Int, nowMs: Long): Boolean {
        val parts = token?.split('.') ?: return false
        if (parts.size != 2 || !MessageDigest.isEqual(sign(parts[0]).toByteArray(), parts[1].toByteArray())) return false
        val fields = runCatching { String(b64d.decode(parts[0])).split('|') }.getOrNull() ?: return false
        if (fields.size != 3) return false
        return fields[0] == scope && fields[1].toIntOrNull() == epoch && (fields[2].toLongOrNull() ?: 0) > nowMs
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return b64.encodeToString(mac.doFinal(payload.toByteArray()))
    }

    companion object {
        const val DEVICE = "device"
        fun share(id: String) = "share:$id"
    }
}
