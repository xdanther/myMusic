package com.mytv.mymusic.data.subsonic

import com.mytv.mymusic.data.AlbumDetail
import com.mytv.mymusic.data.AlbumSummary
import com.mytv.mymusic.data.Playlist
import com.mytv.mymusic.data.ProviderInfo
import com.mytv.mymusic.data.Song
import com.mytv.mymusic.data.native.AccountInfo
import com.mytv.mymusic.data.native.AudioQuality
import com.mytv.mymusic.data.native.BaseNativeProvider
import com.mytv.mymusic.data.native.SongSearchPage
import java.io.IOException

internal const val NAVIDROME_ID = "navidrome"

/**
 * 自建 Navidrome（或任意兼容 Subsonic 的服务器）：账号是服务器地址 + 用户名 + 密码，
 * 不是平台 Cookie。密码只在 [NavidromeStore] 里（Keystore 加密），设置里存的标记只用于显示登录状态。
 *
 * 地址、搜索结果里的歌曲都不带令牌：播放地址在取流时现签名，封面由 [NavidromeImages] 在请求时补认证。
 */
private val NAVIDROME_INFO = ProviderInfo(
    id = NAVIDROME_ID,
    name = "Navidrome",
    search = true,
    download = true,
    lyrics = true,
    searchPlaylist = true,
    playlistSongs = true,
    userPlaylists = true,
    searchAlbum = true,
    searchArtist = true,
    albumSongs = true,
    passwordLogin = true,
    serverLogin = true,
    note = "Your own Navidrome / Subsonic server; enter the server address, username and password to sign in",
)

class NavidromeProvider(
    private val account: () -> NavidromeAccount? = NavidromeStore::current,
) : BaseNativeProvider(NAVIDROME_INFO) {

    override val supportsSearchPagination: Boolean get() = true

    override suspend fun searchSongs(cookie: String, keyword: String): List<Song> =
        searchSongsPage(cookie, keyword, 1, SEARCH_PAGE_SIZE).songs

    override suspend fun searchSongsPage(cookie: String, keyword: String, page: Int, limit: Int): SongSearchPage {
        if (keyword.isBlank()) return SongSearchPage(emptyList(), page, false)
        val size = limit.coerceIn(1, SEARCH_PAGE_SIZE)
        val api = api()
        val result = api.search(
            keyword = keyword,
            songCount = size,
            songOffset = ((page - 1).coerceAtLeast(0)) * size,
        )
        return SongSearchPage(result.songs.map { toSong(it, api) }, page, result.songs.size >= size)
    }

    override suspend fun searchAlbums(cookie: String, keyword: String): List<Playlist> {
        if (keyword.isBlank()) return emptyList()
        val api = api()
        return api.search(keyword, songCount = 0, artistCount = 0).albums.map { toPlaylist(it, api) }
    }

    override suspend fun searchArtists(cookie: String, keyword: String): List<Playlist> {
        if (keyword.isBlank()) return emptyList()
        val api = api()
        return api.search(keyword, songCount = 0, albumCount = 0).artists.map { artist ->
            Playlist(
                id = artist.id,
                name = artist.name,
                source = NAVIDROME_ID,
                trackCount = artist.albumCount,
                cover = api.coverUrl(artist.coverArt),
            )
        }
    }

    override suspend fun albumSongs(cookie: String, id: String): List<Song> {
        val api = api()
        return api.album(id).songs.map { toSong(it, api) }
    }

    override suspend fun albumDetail(cookie: String, id: String): AlbumDetail {
        val api = api()
        val detail = api.album(id)
        return AlbumDetail(album = toAlbumSummary(detail.album, api), songs = detail.songs.map { toSong(it, api) })
    }

    override suspend fun playlistSongs(cookie: String, id: String): List<Song> {
        val api = api()
        return api.playlist(id).songs.map { toSong(it, api) }
    }

    override suspend fun userPlaylists(cookie: String, page: Int, limit: Int): List<Playlist> {
        if (page < 1 || limit < 1) return emptyList()
        val api = api()
        val all = api.playlists().map { playlist ->
            Playlist(
                id = playlist.id,
                name = playlist.name,
                cover = api.coverUrl(playlist.coverArt),
                trackCount = playlist.songCount,
                creator = playlist.owner,
                description = playlist.description,
                source = NAVIDROME_ID,
                extra = mapOf("owner" to playlist.owner),
            )
        }
        val from = (page - 1) * limit
        if (from >= all.size) return emptyList()
        return all.subList(from, minOf(from + limit, all.size))
    }

    override suspend fun searchPlaylists(cookie: String, keyword: String): List<Playlist> {
        val value = keyword.trim()
        if (value.isEmpty()) return emptyList()
        val api = api()
        return api.playlists()
            .filter { it.name.contains(value, ignoreCase = true) }
            .map { playlist ->
                Playlist(
                    id = playlist.id,
                    name = playlist.name,
                    cover = api.coverUrl(playlist.coverArt),
                    trackCount = playlist.songCount,
                    creator = playlist.owner,
                    description = playlist.description,
                    source = NAVIDROME_ID,
                )
            }
    }

    override suspend fun lyrics(cookie: String, song: Song): String =
        if (song.id.isBlank()) "" else api().lyrics(song.id)

    override suspend fun downloadUrl(cookie: String, song: Song, quality: AudioQuality): String =
        api().streamUrl(song.id, maxBitRate(quality))

    override suspend fun passwordLogin(server: String, username: String, password: String): String {
        val account = NavidromeAccount(server, username, password)
        val api = account.api() ?: throw IOException("Server address is invalid")
        api.ping()
        NavidromeStore.save(account)
        return account.normalized().marker()
    }

    override suspend fun accountInfo(cookie: String): AccountInfo? {
        val account = account() ?: return null
        return AccountInfo(nickname = account.username, detail = account.hostLabel)
    }

    // ------------------------------------------------------------ 内部

    private fun api(): SubsonicApi = account()?.api()
        ?: throw IOException("Sign in to your Navidrome server first")

    private fun toSong(item: SubsonicSong, api: SubsonicApi): Song = Song(
        id = item.id,
        name = item.title,
        artist = item.artist,
        album = item.album,
        albumId = item.albumId,
        duration = item.duration,
        size = item.size,
        bitrate = item.bitrate,
        source = NAVIDROME_ID,
        // 播放地址在取流时才签名，搜索结果里不存令牌
        url = "",
        ext = item.suffix,
        cover = api.coverUrl(item.coverArt.ifBlank { item.albumId }),
        link = "",
    )

    private fun toAlbumSummary(item: SubsonicAlbum, api: SubsonicApi): AlbumSummary = AlbumSummary(
        id = item.id,
        name = item.name,
        source = NAVIDROME_ID,
        cover = api.coverUrl(item.coverArt),
        artist = item.artist,
        publishTime = item.year.takeIf { it > 0 }?.toString().orEmpty(),
        trackCount = item.songCount,
    )

    private fun toPlaylist(item: SubsonicAlbum, api: SubsonicApi): Playlist = Playlist(
        id = item.id,
        name = item.name,
        cover = api.coverUrl(item.coverArt),
        trackCount = item.songCount,
        creator = item.artist,
        source = NAVIDROME_ID,
    )

    /** Subsonic 的 maxBitRate 只在用户明确选了低档时传，0 表示原样给文件。 */
    private fun maxBitRate(quality: AudioQuality): Int = when (quality) {
        AudioQuality.Standard -> 128
        AudioQuality.Higher -> 192
        AudioQuality.High -> 320
        else -> 0
    }

    private companion object {
        const val SEARCH_PAGE_SIZE = 50
    }
}
