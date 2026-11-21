package com.mytv.mymusic.data.subsonic

import android.content.Context
import com.mytv.mymusic.data.sync.SecretStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 一个 Navidrome / Subsonic 服务器账号：地址、用户名、密码。
 *
 * 和平台 Cookie 不一样，这里的是真正的服务器凭据，所以密码只留在本机（[SecretStore]，Keystore 加密），
 * 不进 `settings.cookies`、也不进同步文件；[NavidromeStore.marker] 只是一段公开的标记，
 * 让设置页知道“这个源配过服务器”。
 */
data class NavidromeAccount(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    /** 规范化后的服务器地址（无结尾斜杠、保留路径前缀）；地址写错时为 null。 */
    val baseUrl: HttpUrl? get() = parseServer(serverUrl)

    /** 卡片上显示的主机名和端口，例如 `music.example.com:4533`。 */
    val hostLabel: String
        get() = baseUrl?.let { url ->
            val host = if (url.host.contains(':')) "[${url.host}]" else url.host
            if (url.port == HttpUrl.defaultPort(url.scheme)) host else "$host:${url.port}"
        } ?: serverUrl.trim()

    fun normalized(): NavidromeAccount = copy(
        serverUrl = baseUrl?.toString()?.trimEnd('/') ?: serverUrl.trim(),
        username = username.trim(),
    )

    /** 设置页存的标记：能看出配的是哪台服务器、哪个账号，但不含密码。 */
    fun marker(): String = "$MARKER_PREFIX$hostLabel|$username"

    fun api(): SubsonicApi? = baseUrl?.let { SubsonicApi(it, username, password) }

    override fun toString(): String = "NavidromeAccount(server=$serverUrl, user=$username)"

    companion object {
        private const val MARKER_PREFIX = "navidrome:"

        /**
         * 用户常常只写主机名（局域网里的服务器也不会写 https），这里补协议、去掉结尾斜杠和查询串；
         * 路径前缀保留（`https://host/navidrome` 这种反代部署）。
         */
        fun parseServer(input: String): HttpUrl? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull() ?: return null
            if (url.host.isBlank()) return null
            val builder = HttpUrl.Builder().scheme(url.scheme).host(url.host).port(url.port)
            url.pathSegments.filter { it.isNotEmpty() }.forEach { builder.addPathSegment(it) }
            return builder.build()
        }

        /** 从 [marker] 里读出服务器和用户名；不是 Navidrome 的标记时返回 null。 */
        fun parseMarker(marker: String): Pair<String, String>? {
            if (!marker.startsWith(MARKER_PREFIX)) return null
            val body = marker.removePrefix(MARKER_PREFIX)
            val host = body.substringBefore('|', "")
            val user = body.substringAfter('|', "")
            return if (host.isBlank()) null else host to user
        }
    }
}

/**
 * 本机保存的 Navidrome 账号。地址和用户名放普通偏好，密码交给 [SecretStore]。
 *
 * 提供者（[NavidromeProvider]）和封面图片加载（[NavidromeImages]）都从这里取当前账号，
 * 所以登录、退出只需改这一处。
 */
object NavidromeStore {
    private const val PREFS = "mymusic_navidrome"
    private const val KEY_SERVER = "server_url"
    private const val KEY_USER = "username"
    private const val SECRET_PASSWORD = "navidrome_password"

    private var context: Context? = null
    private var secrets: SecretStore? = null

    private val _account = MutableStateFlow<NavidromeAccount?>(null)
    val account: StateFlow<NavidromeAccount?> = _account.asStateFlow()

    /** 应用启动时调一次；没调过（单元测试）时所有方法都按“没有账号”处理。 */
    @Synchronized
    fun attach(context: Context, secrets: SecretStore) {
        this.context = context.applicationContext
        this.secrets = secrets
        _account.value = load()
    }

    @Synchronized
    fun current(): NavidromeAccount? = _account.value

    @Synchronized
    fun save(account: NavidromeAccount) {
        val normalized = account.normalized()
        val prefs = preferences() ?: return
        secrets?.saveSecret(SECRET_PASSWORD, normalized.password)
        prefs.edit()
            .putString(KEY_SERVER, normalized.serverUrl)
            .putString(KEY_USER, normalized.username)
            .apply()
        _account.value = normalized
    }

    @Synchronized
    fun clear() {
        secrets?.removeSecret(SECRET_PASSWORD)
        preferences()?.edit()?.clear()?.apply()
        _account.value = null
    }

    private fun load(): NavidromeAccount? {
        val prefs = preferences() ?: return null
        val server = prefs.getString(KEY_SERVER, null)?.takeIf { it.isNotBlank() } ?: return null
        val password = secrets?.secret(SECRET_PASSWORD) ?: return null
        return NavidromeAccount(
            serverUrl = server,
            username = prefs.getString(KEY_USER, "").orEmpty(),
            password = password,
        ).normalized()
    }

    private fun preferences() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
