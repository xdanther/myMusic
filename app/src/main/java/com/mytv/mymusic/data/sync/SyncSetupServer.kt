package com.mytv.mymusic.data.sync

import com.mytv.mymusic.data.webdav.WebDavAccount
import com.mytv.mymusic.data.webdav.WebDavMessages
import com.mytv.mymusic.data.webdav.WebDavPaths
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

internal const val SYNC_SETUP_SESSION_MILLIS = 10 * 60 * 1000L
private const val MAX_CONNECT_BODY_BYTES = 16 * 1024L

/** TV 当前配色会作为 theme.css 发给面板；这里只包含颜色，不包含账号或令牌。 */
internal data class SyncSetupTheme(
    val background: String = "#0F1513",
    val surface: String = "#151B19",
    val surfaceContainer: String = "#1B211F",
    val surfaceContainerHigh: String = "#252C2A",
    val primary: String = "#82D5C7",
    val onPrimary: String = "#003730",
    val primaryContainer: String = "#005047",
    val onPrimaryContainer: String = "#9CF2E2",
    val secondaryContainer: String = "#334B45",
    val onSecondaryContainer: String = "#D0E8E1",
    val tertiaryContainer: String = "#3A4666",
    val onTertiaryContainer: String = "#DCE2FF",
    val onSurface: String = "#DEE4E0",
    val onSurfaceVariant: String = "#BBC8C2",
    val outline: String = "#86958E",
    val outlineVariant: String = "#3B4944",
    val error: String = "#FFB4AB",
    val onError: String = "#690005",
    val errorContainer: String = "#93000A",
    val onErrorContainer: String = "#FFDAD6",
)

internal data class SyncSetupSnapshot(
    val provider: String = "gist",
    val theme: SyncSetupTheme = SyncSetupTheme(),
    val sync: SyncUiState = SyncUiState(),
)

/**
 * 手机提交的配置。WebDAV 时 [token] 是整个账号 [WebDavAccount.encode] 出来的串，
 * 交给 SyncCoordinator.connect 的方式和 Gist 令牌一样；[webDav] 只用来在会话里做校验。
 */
internal class SyncSetupSubmission(
    val provider: String,
    val token: String,
    val remote: String,
    val webDav: WebDavAccount? = null,
)

internal typealias SyncSetupStarted = LanPanelStarted

internal enum class SyncSetupPhase(val wireValue: String) {
    Idle("idle"),
    Working("working"),
    NeedsTv("needs_tv"),
    Success("success"),
    Error("error"),
}

internal data class SyncSetupStatus(
    val phase: SyncSetupPhase,
    val title: String,
    val detail: String,
    val provider: String,
)

internal enum class SyncSetupSubmitResult { Accepted, Busy, Invalid, Expired, Failed }

@Serializable
private class ConnectRequest(
    val provider: String = "",
    val token: String = "",
    val remote: String = "",
    // 以下是 WebDAV 表单；令牌类的提交里没有这些字段
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val directory: String = "",
    val trustSelfSigned: Boolean = false,
) {
    fun toSubmission(): SyncSetupSubmission = if (provider == WebDavSyncProvider.ID) {
        val account = WebDavAccount(
            serverUrl = url,
            username = username,
            password = password,
            directory = directory,
            trustSelfSigned = trustSelfSigned,
        )
        SyncSetupSubmission(provider, account.encode(), "", account)
    } else {
        SyncSetupSubmission(provider, token, remote)
    }
}

private data class AccountMarker(
    val provider: String?,
    val remoteId: String?,
    val updatedAt: Long,
)

/**
 * 网页会话只记录提交来源和同步进度，不保留提交的令牌。
 * 已连接账户只有在本次连接实际开始或账户发生变化后才算成功，避免换令牌时误报旧账户。
 */
internal class SyncSetupSession(
    private val snapshot: () -> SyncSetupSnapshot,
    private val onSubmit: (SyncSetupSubmission) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
    sessionMillis: Long = SYNC_SETUP_SESSION_MILLIS,
    val accessToken: String = newPanelToken(),
) {
    val expiresAt: Long = clock() + sessionMillis

    private var submittedProvider: String? = null
    private var submissionError = ""
    private var baselineAccount = AccountMarker(null, null, 0)
    private var baselineError = ""
    private var submittedAt = 0L
    private var observedTransition = false

    @Synchronized
    fun submit(submission: SyncSetupSubmission): SyncSetupSubmitResult {
        if (isExpired()) return SyncSetupSubmitResult.Expired
        val webDav = submission.webDav?.normalized()
        val token = if (webDav != null) webDav.encode() else submission.token.trim()
        val valid = when {
            submission.provider !in PROVIDERS -> false
            submission.provider == WebDavSyncProvider.ID -> webDav != null && isValidWebDav(webDav)
            else -> token.isNotBlank() && token.length <= MAX_TOKEN_CHARS &&
                submission.remote.length <= MAX_REMOTE_CHARS
        }
        if (!valid) return SyncSetupSubmitResult.Invalid
        if (submittedProvider == null && snapshot().sync.status == SyncStatus.Syncing) {
            return SyncSetupSubmitResult.Busy
        }
        submittedProvider?.let {
            if (status().phase in setOf(SyncSetupPhase.Working, SyncSetupPhase.NeedsTv, SyncSetupPhase.Success)) {
                return SyncSetupSubmitResult.Busy
            }
        }
        val currentSync = snapshot().sync
        val account = currentSync.account
        baselineAccount = AccountMarker(account?.provider, account?.remoteId, account?.updatedAt ?: 0)
        baselineError = currentSync.error
        submittedAt = clock()
        submittedProvider = submission.provider
        submissionError = ""
        observedTransition = false
        return try {
            onSubmit(SyncSetupSubmission(submission.provider, token, submission.remote.trim(), webDav))
            SyncSetupSubmitResult.Accepted
        } catch (_: Exception) {
            submissionError = "The TV didn't accept this setup; go back to the TV and retry."
            SyncSetupSubmitResult.Failed
        }
    }

    @Synchronized
    fun status(): SyncSetupStatus {
        val current = snapshot()
        val provider = submittedProvider ?: return SyncSetupStatus(
            phase = SyncSetupPhase.Idle,
            title = "Waiting for setup",
            detail = "Enter an access token or WebDAV account and the TV will verify and connect.",
            provider = current.provider.takeIf { it in PROVIDERS } ?: "gist",
        )
        val sync = current.sync
        val account = sync.account
        val accountMarker = AccountMarker(account?.provider, account?.remoteId, account?.updatedAt ?: 0)
        if (sync.status == SyncStatus.Syncing || sync.initialMerge != null ||
            accountMarker != baselineAccount || sync.error != baselineError
        ) {
            observedTransition = true
        }
        return when {
            submissionError.isNotBlank() -> SyncSetupStatus(
                SyncSetupPhase.Error, "Connection incomplete", submissionError, provider,
            )
            sync.initialMerge != null -> SyncSetupStatus(
                SyncSetupPhase.NeedsTv,
                "Continue on the TV",
                "This device and the cloud both have playlists. Choose how to merge on the TV; this page updates automatically.",
                provider,
            )
            sync.status == SyncStatus.Syncing -> SyncSetupStatus(
                SyncSetupPhase.Working,
                "Connecting",
                sync.phase.ifBlank { "The TV is verifying the account and reading the cloud file." },
                provider,
            )
            sync.error.isNotBlank() && (observedTransition || clock() - submittedAt >= STALE_ERROR_GRACE_MILLIS) ->
                SyncSetupStatus(
                    SyncSetupPhase.Error,
                    "Connection incomplete",
                    // WebDAV 的错误文案是固定写法（不夹服务器正文），手机上要看到才知道改哪一项
                    if (provider == WebDavSyncProvider.ID) sync.error else safeSetupError(sync.error),
                    provider,
                )
            observedTransition && account?.provider == provider && sync.error.isBlank() -> SyncSetupStatus(
                SyncSetupPhase.Success,
                "Connected",
                successDetail(provider, sync),
                provider,
            )
            else -> SyncSetupStatus(
                SyncSetupPhase.Working,
                "Waiting for the TV",
                "Setup sent; the TV will show progress shortly.",
                provider,
            )
        }
    }

    fun isExpired(): Boolean = clock() >= expiresAt

    /**
     * 手机网页读不到 WebDAV 服务器的证书，打开“信任自签名证书”时由电视第一次连上时自己记下那张；
     * 把指纹也报给手机，方便对着服务器核对。
     */
    private fun successDetail(provider: String, sync: SyncUiState): String {
        val certificate = sync.webDav?.certificate?.takeIf { provider == WebDavSyncProvider.ID && !it.systemTrusted }
            ?: return "The TV is connected to ${providerLabel(provider)}; you can close this page."
        return "The TV connected to WebDAV and saved the server's ${WebDavMessages.certificate(certificate)}." +
            "If the certificate changes later the TV will stop and warn you. You can close this page."
    }

    private fun isValidWebDav(account: WebDavAccount): Boolean =
        WebDavPaths.parseServerUrl(account.serverUrl) != null &&
            account.serverUrl.length <= MAX_REMOTE_CHARS &&
            account.username.isNotBlank() && account.username.length <= MAX_FIELD_CHARS &&
            account.password.isNotEmpty() && account.password.length <= MAX_TOKEN_CHARS &&
            account.directory.length <= MAX_FIELD_CHARS

    private fun safeSetupError(message: String): String = when {
        message.startsWith("Invalid token") || message.startsWith("Token permissions") || message.startsWith("The token can't read") -> message
        message.startsWith("Cloud file not found") || message.startsWith("Sync file") || message.startsWith("Cloud") -> message
        message.startsWith("Network unreachable") -> "Network unreachable; check the TV's connection and try again."
        message.startsWith("The sync service returned an error") -> "The sync service is unavailable; try again later. The TV shows the details."
        else -> "The TV couldn't finish connecting. Check the error on the TV, fix it, then retry here."
    }

    private companion object {
        val PROVIDERS = setOf("gist", "gitee", WebDavSyncProvider.ID)
        const val MAX_TOKEN_CHARS = 1024
        const val MAX_FIELD_CHARS = 512
        const val MAX_REMOTE_CHARS = 2048
        const val STALE_ERROR_GRACE_MILLIS = 3000L
    }
}

internal data class SetupAddressCandidate(val interfaceName: String, val address: InetAddress)

/**
 * 挑选要写进二维码的局域网 IPv4。
 *
 * 一台设备常同时有多个 IPv4：开着 Wi-Fi Direct 时 p2p0 会拿到 192.168.49.1 这种和家庭局域网
 * 不同网段的地址，USB / 蓝牙共享网络（usb0、rndis0、bt-pan）、蜂窝数据（rmnet*）、
 * VPN / TUN（tun*、tap*）、Wi-Fi Aware（aware*）也各有地址。只有 Wi-Fi / 以太网那个是手机
 * 所在局域网能访问的，所以按网卡名排优先级，前面的网卡没有可用地址时才退到后面的——只靠
 * USB 共享手机网络上网时，usb0 / rndis0 就是唯一入口，不能把它排除掉。
 *
 * 只展示私网 IPv4：169.254.x.x 是链路本地，按它访问不到，公网地址写进二维码也没有意义。
 * 同一块网卡报了多个地址时保留系统枚举顺序，第一个通常是 DHCP 分下来的那个，不按字符串
 * 比大小——那样 192.168.49.1 会排在 192.168.50.64 前面。
 */
internal fun selectSyncSetupAddress(candidates: List<SetupAddressCandidate>): InetAddress? = candidates
    .filter { candidate ->
        val address = candidate.address
        address is Inet4Address && address.isSiteLocalAddress &&
            !address.isLoopbackAddress && !address.isLinkLocalAddress && !address.isAnyLocalAddress
    }
    .sortedWith(
        compareBy<SetupAddressCandidate> { it.interfaceName.lowercase().lanPriority() }
            .thenBy { it.interfaceName },
    )
    .firstOrNull()?.address

/** Wi-Fi / 以太网网卡：手机所在局域网的入口。其余（p2p、usb/rndis、rmnet、tun…）都排在后面。 */
private fun String.lanPriority(): Int {
    val name = lowercase()
    return when {
        name.startsWith("wlan") -> 0
        name.startsWith("eth") || name.startsWith("en") -> 1
        // USB / 蓝牙共享网络：没有 Wi-Fi 和有线时，手机往往就在这个共享网络里
        name.startsWith("usb") || name.startsWith("rndis") || name.startsWith("bt-pan") -> 2
        else -> 3
    }
}

/**
 * 枚举网卡取要展示给手机的地址。服务本身监听通配地址（bindHost 为空时 NanoHTTPD 绑
 * 0.0.0.0），所有网卡都进得来，所以这里只决定二维码和页面上写哪个地址。
 */
internal fun findSyncSetupAddress(): InetAddress? = runCatching {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
    Collections.list(interfaces)
        .filter { it.isUp && !it.isLoopback }
        .flatMap { network ->
            Collections.list(network.inetAddresses).map { SetupAddressCandidate(network.name, it) }
        }
        .let(::selectSyncSetupAddress)
}.getOrNull()

/**
 * 仅在电视连接页运行的局域网 HTTP 面板。
 * 二维码持有随机会话路径；接口只接受同源 JSON POST，不开放令牌读取或任意文件访问。
 * 会话、来源、过期、安全头这些公共部分在 [LanPanelServer]。
 */
internal class SyncSetupServer private constructor(
    assets: (String) -> ByteArray?,
    private val snapshot: () -> SyncSetupSnapshot,
    private val session: SyncSetupSession,
    bindHost: String?,
) : LanPanelServer(bindHost, assets, session.accessToken) {
    constructor(
        assets: (String) -> ByteArray?,
        snapshot: () -> SyncSetupSnapshot,
        onSubmit: (SyncSetupSubmission) -> Unit,
        bindHost: String? = null,
        clock: () -> Long = System::currentTimeMillis,
        sessionMillis: Long = SYNC_SETUP_SESSION_MILLIS,
    ) : this(assets, snapshot, SyncSetupSession(snapshot, onSubmit, clock, sessionMillis), bindHost)

    private val json = Json { ignoreUnknownKeys = true }

    override val expiresAt: Long get() = session.expiresAt

    override fun isExpired(): Boolean = session.isExpired()

    override fun route(http: IHTTPSession, route: String): Response = when (http.method) {
        Method.GET -> get(route)
        Method.POST -> if (route == "api/connect") {
            connect(http)
        } else {
            errorResponse(Response.Status.NOT_FOUND, "Endpoint not found")
        }
        else -> errorResponse(Response.Status.METHOD_NOT_ALLOWED, "Unsupported request method")
    }

    private fun get(route: String): Response = when (route) {
        "", "index.html" -> asset("index.html", "text/html; charset=UTF-8", html = true)
        "setup.css" -> asset("setup.css", "text/css; charset=UTF-8")
        "setup.js" -> asset("setup.js", "application/javascript; charset=UTF-8")
        "theme.css" -> textResponse(Response.Status.OK, "text/css; charset=UTF-8", themeCss(snapshot().theme))
        "api/session" -> jsonResponse(
            Response.Status.OK,
            buildJsonObject {
                put("provider", snapshot().provider.takeIf { it in SETUP_PROVIDERS } ?: "gist")
                put("expiresAt", session.expiresAt)
            },
        )
        "api/status" -> jsonResponse(Response.Status.OK, statusJson(session.status()))
        else -> errorResponse(Response.Status.NOT_FOUND, "Resource not found")
    }

    private fun connect(http: IHTTPSession): Response {
        http.checkWrite(
            customHeader = "x-mymusic-setup",
            contentType = "application/json",
            maxBytes = MAX_CONNECT_BODY_BYTES,
            crossSiteMessage = "Submit from the setup page the TV provides",
            badRequestMessage = "Use the connect button on the setup page",
        )?.let { return it }
        val body = mutableMapOf<String, String>()
        try {
            http.parseBody(body)
        } catch (_: Exception) {
            return errorResponse(Response.Status.BAD_REQUEST, "Can't read the configuration")
        }
        val raw = body["postData"].orEmpty()
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_CONNECT_BODY_BYTES) {
            return errorResponse(Response.Status.BAD_REQUEST, "The request is too large")
        }
        val request = try {
            json.decodeFromString<ConnectRequest>(raw)
        } catch (_: Exception) {
            return errorResponse(Response.Status.BAD_REQUEST, "Invalid setup format")
        }
        return when (session.submit(request.toSubmission())) {
            SyncSetupSubmitResult.Accepted -> jsonResponse(
                Response.Status.ACCEPTED,
                statusJson(
                    SyncSetupStatus(
                        SyncSetupPhase.Working,
                        "Setup sent",
                        "The TV received the setup and is verifying it.",
                        request.provider,
                    ),
                ),
            )
            SyncSetupSubmitResult.Busy -> errorResponse(Response.Status.CONFLICT, "The TV is applying the setup; please wait")
            SyncSetupSubmitResult.Invalid -> errorResponse(
                Response.Status.BAD_REQUEST,
                if (request.provider == WebDavSyncProvider.ID) {
                    "Check the WebDAV server address, username and password"
                } else {
                    "Check the service type and token; an existing file ID or link can't exceed 2048 characters"
                },
            )
            SyncSetupSubmitResult.Expired -> errorResponse(Response.Status.GONE, "The QR code expired; scan again")
            SyncSetupSubmitResult.Failed -> errorResponse(Response.Status.INTERNAL_ERROR, "The TV didn't accept the setup; try again")
        }
    }

    private fun statusJson(status: SyncSetupStatus): JsonObject = buildJsonObject {
        put("state", status.phase.wireValue)
        put("title", status.title)
        put("detail", status.detail)
        put("provider", status.provider)
    }

    private companion object {
        val SETUP_PROVIDERS = setOf("gist", "gitee", WebDavSyncProvider.ID)
    }
}

internal fun providerLabel(provider: String): String = when (provider) {
    "gist" -> "GitHub Gist"
    "gitee" -> "Gitee snippets"
    WebDavSyncProvider.ID -> "WebDAV"
    else -> provider
}
