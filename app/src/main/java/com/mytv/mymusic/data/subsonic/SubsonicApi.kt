package com.mytv.mymusic.data.subsonic

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Subsonic 协议版本：Navidrome 全部支持 1.16.1 的方法（含 search3 / getAlbumList2）。 */
internal const val SUBSONIC_API_VERSION = "1.16.1"

/** 服务端日志里显示的客户端名字。 */
internal const val SUBSONIC_CLIENT_NAME = "myMusic"

/** 服务器回了 `status: failed`。[code] 是 Subsonic 错误码（40 用户名密码不对、70 找不到……）。 */
class SubsonicException(val code: Int, message: String) : IOException(message)

/** Subsonic 的一首歌；和 [com.mytv.mymusic.data.Song] 分开，协议字段不混进应用模型。 */
data class SubsonicSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val duration: Int,
    val size: Long,
    val bitrate: Int,
    val suffix: String,
    val coverArt: String,
    val track: Int = 0,
)

data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val songCount: Int,
    val duration: Int,
    val coverArt: String,
    val year: Int = 0,
)

data class SubsonicArtist(val id: String, val name: String, val albumCount: Int, val coverArt: String = "")

data class SubsonicPlaylist(
    val id: String,
    val name: String,
    val songCount: Int,
    val coverArt: String,
    val description: String = "",
    val owner: String = "",
)

data class SubsonicSearch(
    val songs: List<SubsonicSong> = emptyList(),
    val albums: List<SubsonicAlbum> = emptyList(),
    val artists: List<SubsonicArtist> = emptyList(),
)

data class SubsonicAlbumDetail(val album: SubsonicAlbum, val songs: List<SubsonicSong>)

data class SubsonicPlaylistDetail(val playlist: SubsonicPlaylist, val songs: List<SubsonicSong>)

/**
 * Subsonic API 客户端（Navidrome、Airsonic、Gonic 这些服务器都实现这个协议）。
 *
 * 认证是每个请求现算的：`t = md5(密码 + 随机盐)`、`s = 盐`，密码本身从不发出去。
 * 所以流播 / 封面地址可以在播放或加载图片时临时拼，不需要在搜索结果里存令牌。
 *
 * [baseUrl] 可以带路径前缀（`https://host/navidrome`），所有方法都拼在它下面的 `rest/`。
 */
class SubsonicApi(
    val baseUrl: HttpUrl,
    val username: String,
    val password: String,
    private val client: OkHttpClient = defaultClient,
) {
    suspend fun ping() {
        request("ping")
    }

    /** 搜索歌曲 / 专辑 / 歌手；[songOffset] 用于翻页。 */
    suspend fun search(
        keyword: String,
        songCount: Int = 50,
        songOffset: Int = 0,
        albumCount: Int = 20,
        artistCount: Int = 10,
    ): SubsonicSearch {
        val root = request(
            "search3",
            mapOf(
                "query" to keyword,
                "songCount" to songCount.toString(),
                "songOffset" to songOffset.toString(),
                "albumCount" to albumCount.toString(),
                "artistCount" to artistCount.toString(),
            ),
        )
        val result = root.obj("searchResult3") ?: return SubsonicSearch()
        return SubsonicSearch(
            songs = result.items("song").map(::song),
            albums = result.items("album").map(::album),
            artists = result.items("artist").map(::artist),
        )
    }

    /** 专辑列表；[type] 用 Subsonic 的类型名（recent / newest / alphabeticalByName / random……）。 */
    suspend fun albumList(type: String = "recent", size: Int = 100, offset: Int = 0): List<SubsonicAlbum> {
        val root = request(
            "getAlbumList2",
            mapOf("type" to type, "size" to size.toString(), "offset" to offset.toString()),
        )
        return root.obj("albumList2")?.items("album")?.map(::album).orEmpty()
    }

    suspend fun album(id: String): SubsonicAlbumDetail {
        val root = request("getAlbum", mapOf("id" to id))
        val node = root.obj("album") ?: throw SubsonicException(70, "Album not found")
        return SubsonicAlbumDetail(album = album(node), songs = node.items("song").map(::song))
    }

    suspend fun playlists(): List<SubsonicPlaylist> {
        val root = request("getPlaylists")
        return root.obj("playlists")?.items("playlist")?.map(::playlist).orEmpty()
    }

    suspend fun playlist(id: String): SubsonicPlaylistDetail {
        val root = request("getPlaylist", mapOf("id" to id))
        val node = root.obj("playlist") ?: throw SubsonicException(70, "Playlist not found")
        return SubsonicPlaylistDetail(playlist = playlist(node), songs = node.items("entry").map(::song))
    }

    /**
     * 歌词（OpenSubsonic 的 getLyricsBySongId）。返回的格式和平台歌词一致：
     * `[mm:ss.xx]歌词` 一行一句，有时间轴就带时间。服务器没实现或没有歌词时返回空串。
     */
    suspend fun lyrics(songId: String): String {
        val root = try {
            request("getLyricsBySongId", mapOf("id" to songId))
        } catch (error: SubsonicException) {
            // 老服务器没有这个方法会回 0/70，当成“没有歌词”，不影响播放
            return ""
        }
        val lines = root.obj("lyricsList")
            ?.items("structuredLyrics")
            ?.flatMap { lyric -> lyric.items("line") }
            .orEmpty()
        return lines.mapNotNull { line ->
            val value = line.str("value").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val startMs = line.data("start")?.toLongOrNull()
            if (startMs == null) value else "${formatTime(startMs)} $value"
        }.joinToString("\n")
    }

    /** 直接给播放器的地址；每次调用重新签名。 */
    fun streamUrl(songId: String, maxBitRate: Int = 0): String {
        val builder = endpoint("stream")
        authParams().forEach { (key, value) -> builder.addQueryParameter(key, value) }
        builder.addQueryParameter("id", songId)
        if (maxBitRate > 0) builder.addQueryParameter("maxBitRate", maxBitRate.toString())
        builder.addQueryParameter("estimateContentLength", "true")
        return builder.build().toString()
    }

    /** 封面地址。不带令牌：图片请求由 [NavidromeImages] 在发出去之前补上认证。 */
    fun coverUrl(coverArtId: String, size: Int = 600): String {
        if (coverArtId.isBlank()) return ""
        val builder = endpoint("getCoverArt")
        builder.addQueryParameter("id", coverArtId)
        if (size > 0) builder.addQueryParameter("size", size.toString())
        return builder.build().toString()
    }

    // ------------------------------------------------------------ 请求

    private fun endpoint(method: String): HttpUrl.Builder = baseUrl.newBuilder().addPathSegments("rest/$method.view")

    private fun authParams(): List<Pair<String, String>> =
        SubsonicAuth.params(username, password) + ("f" to "json")

    private suspend fun request(method: String, params: Map<String, String> = emptyMap()): JsonObject {
        val builder = endpoint(method)
        authParams().forEach { (key, value) -> builder.addQueryParameter(key, value) }
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val body = execute(Request.Builder().url(builder.build()).get().header("Accept", "application/json").build())
        val parsed = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
            ?: throw SubsonicException(0, "Unexpected response from the server")
        val root = parsed["subsonic-response"]?.let { runCatching { it.jsonObject }.getOrNull() }
            ?: throw SubsonicException(0, "Unexpected response from the server")
        if (root.str("status") != "ok") {
            val error = root.obj("error")
            throw SubsonicException(
                code = error?.data("code")?.toIntOrNull() ?: 0,
                message = error?.str("message")?.takeIf { it.isNotBlank() } ?: "The server rejected the request",
            )
        }
        return root
    }

    private suspend fun execute(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call: Call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                    if (!it.isSuccessful) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                SubsonicException(it.code, "HTTP ${it.code} ${it.message}"),
                            )
                        }
                        return
                    }
                    if (continuation.isActive) continuation.resume(body)
                }
            }
        })
    }

    // ------------------------------------------------------------ 映射

    private fun song(node: JsonObject) = SubsonicSong(
        id = node.str("id"),
        title = node.str("title").ifBlank { node.str("name") },
        artist = node.str("artist"),
        album = node.str("album"),
        albumId = node.str("albumId"),
        duration = node.data("duration")?.toDoubleOrNull()?.toInt() ?: 0,
        size = node.data("size")?.toLongOrNull() ?: 0,
        bitrate = node.data("bitRate")?.toIntOrNull() ?: 0,
        suffix = node.str("suffix"),
        coverArt = node.str("coverArt"),
        track = node.data("track")?.toIntOrNull() ?: 0,
    )

    private fun album(node: JsonObject) = SubsonicAlbum(
        id = node.str("id"),
        name = node.str("name").ifBlank { node.str("title") },
        artist = node.str("artist"),
        songCount = node.data("songCount")?.toIntOrNull() ?: 0,
        duration = node.data("duration")?.toIntOrNull() ?: 0,
        coverArt = node.str("coverArt").ifBlank { node.str("id") },
        year = node.data("year")?.toIntOrNull() ?: 0,
    )

    private fun artist(node: JsonObject) = SubsonicArtist(
        id = node.str("id"),
        name = node.str("name"),
        albumCount = node.data("albumCount")?.toIntOrNull() ?: 0,
        coverArt = node.str("artistImageUrl").ifBlank { node.str("coverArt") },
    )

    private fun playlist(node: JsonObject) = SubsonicPlaylist(
        id = node.str("id"),
        name = node.str("name"),
        songCount = node.data("songCount")?.toIntOrNull() ?: 0,
        coverArt = node.str("coverArt").ifBlank { node.str("id") },
        description = node.str("comment"),
        owner = node.str("owner"),
    )

    companion object {
        /** 封面和歌词要等服务器读文件，比普通接口宽松一点。 */
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        /** `mm:ss.xx`，和 LRC 的时间轴一致。 */
        private fun formatTime(millis: Long): String {
            val totalSeconds = (millis / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hundredths = (millis % 1000) / 10
            return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
        }
    }
}

// ---------------------------------------------------------------- JSON 小工具

/**
 * 字段可能是数字也可能是字符串（Subsonic 各服务器不统一），统一取原始文本再自行转换。
 */
internal fun JsonObject.data(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.str(key: String): String = data(key).orEmpty()

internal fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)

internal fun JsonObject.items(key: String): List<JsonObject> = when (val node = this[key]) {
    is JsonArray -> node.mapNotNull { it as? JsonObject }
    is JsonObject -> listOf(node)
    else -> emptyList()
}
