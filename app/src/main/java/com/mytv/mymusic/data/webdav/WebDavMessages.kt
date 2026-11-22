package com.mytv.mymusic.data.webdav

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 给用户看的 WebDAV 错误文案。同步位置和以后的上传共用，所以集中在这里。
 * 文案只写原因和下一步，不带服务器返回的正文（可能很长，也可能夹着内部路径）。
 */
object WebDavMessages {
    fun isJianguoyun(account: WebDavAccount?): Boolean =
        account?.baseUrl?.host?.endsWith("jianguoyun.com", ignoreCase = true) == true

    fun describe(error: WebDavException, account: WebDavAccount?): String = when (error.kind) {
        WebDavErrorKind.Unauthorized -> if (isJianguoyun(account)) {
            "Wrong username or password. For Nutstore use the app password from Security options › Third-party app management, not your login password."
        } else {
            "Wrong username or password."
        }
        WebDavErrorKind.Forbidden ->
            "The server denied access to this directory (403); check the account permissions or pick another directory."
        WebDavErrorKind.NotFound ->
            "That address wasn't found on the server (404); check the server URL."
        WebDavErrorKind.MethodNotAllowed ->
            "The server doesn't support this operation (405); make sure the URL is a WebDAV address."
        WebDavErrorKind.Conflict ->
            "Not a directory, please choose another directory."
        WebDavErrorKind.PreconditionFailed ->
            "The cloud file was just changed by another device; try again."
        WebDavErrorKind.Locked ->
            "The file is locked by the server (423); try again later."
        WebDavErrorKind.PayloadTooLarge ->
            "The server limits upload size per request (413)."
        WebDavErrorKind.InsufficientStorage ->
            "WebDAV is out of space (507)."
        WebDavErrorKind.Http ->
            "The WebDAV server returned an error (HTTP ${error.status})."
        WebDavErrorKind.UntrustedCertificate -> if (account?.trustSelfSigned == true) {
            // 开关开着还失败：系统不认，本机也没确认过这张（或者证书上的名字和地址对不上）
            "The server certificate isn't trusted and doesn't match the one confirmed on this device; connection stopped. $RECHECK"
        } else {
            "The server certificate isn't trusted. For a server you run yourself, enable Trust self-signed certificate and try again."
        }
        WebDavErrorKind.CertificateChanged ->
            "The server certificate changed and differs from the one trusted before; connection stopped. $RECHECK"
        WebDavErrorKind.Tls ->
            "Couldn't establish a secure connection: ${error.message}"
        WebDavErrorKind.Network ->
            "Can't reach the WebDAV server: ${error.message}"
        WebDavErrorKind.InvalidResponse ->
            "The server's response wasn't understood (${error.message}); make sure the URL is a WebDAV address."
        WebDavErrorKind.InvalidConfig ->
            error.message ?: "WebDAV settings are incomplete."
    }

    /**
     * 测试连接的结果，写在按钮下方。[certificate] 是这次读到的服务器证书，系统不认时另起一行给出指纹；
     * [pinnedBefore] 表示本机之前就认这张。
     */
    fun checkSummary(
        result: WebDavCheckResult,
        account: WebDavAccount?,
        certificate: WebDavCertificate? = null,
        pinnedBefore: Boolean = false,
    ): String {
        val error = result.error
        val summary = if (!result.ok && error != null) {
            "${result.failedStep?.label ?: "Test"} failed: ${describe(error, account)}"
        } else {
            buildString {
                append("Read and write")
                append(if (result.supportsRange) " · the server supports range requests" else " · the server doesn't support range requests, so uploaded songs need a full download before playing")
                if (result.createdDirectory) append(" · created directory ${account?.normalizedDirectory.orEmpty().ifBlank { "/" }}")
                if (result.foundFile) append(" · the directory already has a sync file; you'll choose how to merge when connecting")
            }
        }
        if (certificate == null || certificate.systemTrusted) return summary
        val note = if (pinnedBefore) "matches the one trusted on this device." else "Compare it with the certificate on the server; this device trusts only this one afterwards."
        return "$summary\n${certificate(certificate)}，$note"
    }

    /** 测试连接时发现证书和本机记下的不一样：这次没发密码，给出新指纹，让用户自己决定信不信。 */
    fun certificateChanged(observed: WebDavCertificate): String =
        "The server certificate changed and differs from the one trusted before. This test stopped before sending the password.\n" +
            "New ${certificate(observed)}\n" +
            "Only tap Trust new certificate if you changed it yourself; otherwise don't connect, someone may be intercepting."

    /** `证书指纹 AB:CD:…（颁发给 nas.local，自签名，2027-03 到期）` */
    fun certificate(certificate: WebDavCertificate, now: Long = System.currentTimeMillis()): String {
        val month = SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date(certificate.notAfter))
        val validity = if (certificate.notAfter < now) "expired $month" else "expires $month"
        val issuer = if (certificate.selfSigned) "Self-signed" else "Issued by ${certificate.issuer}"
        return "Certificate fingerprint ${certificate.fingerprint} (issued to ${certificate.subject}, $issuer, $validity)"
    }

    private const val RECHECK = "If you changed the certificate yourself, tap Test connection in WebDAV settings to check the new fingerprint before connecting."
}
