package com.mytv.mymusic.data.subsonic

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Subsonic 客户端：签名、错误映射、结果映射，以及“地址里不带密码/令牌”。 */
class SubsonicApiTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun api() = SubsonicApi(server.url("/"), "alice", "secret", OkHttpClient())

    @Test
    fun searchSignsRequestAndKeepsPasswordOutOfUrls() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """
                {"subsonic-response":{"status":"ok","version":"1.16.1","searchResult3":{
                  "song":[{"id":"s1","title":"Song One","artist":"Alice","album":"First",
                           "albumId":"a1","duration":215,"size":123456,"bitRate":320,
                           "suffix":"flac","coverArt":"al-1"}],
                  "album":[{"id":"a1","name":"First","artist":"Alice","songCount":9,"duration":2400,"coverArt":"al-1"}],
                  "artist":[{"id":"ar1","name":"Alice","albumCount":1}]
                }}}
                """.trimIndent(),
            ).build(),
        )

        val result = api().search("hello")

        val url = server.takeRequest().url
        assertEquals("hello", url.queryParameter("query"))
        assertEquals("alice", url.queryParameter("u"))
        val salt = url.queryParameter("s")
        assertNotNull(salt)
        assertEquals(SubsonicAuth.md5("secret$salt"), url.queryParameter("t"))
        assertFalse(url.toString().contains("secret"))

        assertEquals(1, result.songs.size)
        val song = result.songs.single()
        assertEquals("s1", song.id)
        assertEquals("Song One", song.title)
        assertEquals(215, song.duration)
        assertEquals(320, song.bitrate)
        assertEquals("flac", song.suffix)
        assertEquals(1, result.albums.size)
        assertEquals(9, result.albums.single().songCount)
        assertEquals(1, result.artists.size)
        assertEquals("Alice", result.artists.single().name)
    }

    @Test
    fun coverUrlCarriesNoCredentials() = runTest {
        val url = api().coverUrl("al-1")
        assertTrue(url.contains("getCoverArt.view"))
        assertTrue(url.contains("id=al-1"))
        assertNull("cover URLs must not embed a token", url.substringAfter('?').split('&').firstOrNull { it.startsWith("t=") })
        assertFalse(url.contains("secret"))
    }

    @Test
    fun streamUrlIsSignedOnEachCall() {
        val api = api()
        val first = api.streamUrl("s1")
        Thread.sleep(2)
        val second = api.streamUrl("s1")
        assertTrue(first.contains("stream.view"))
        assertTrue(first.contains("id=s1"))
        assertTrue(first.contains("estimateContentLength=true"))
        assertFalse(first.contains("secret"))
        assertTrue("each call uses a fresh salt", first != second)
    }

    @Test
    fun failedStatusBecomesSubsonicException() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":40,"message":"Wrong username or password"}}}""",
            ).build(),
        )

        val error = runCatching { api().ping() }.exceptionOrNull()
        assertTrue(error is SubsonicException)
        assertEquals(40, (error as SubsonicException).code)
    }

    @Test
    fun playlistEntriesAreSongs() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """
                {"subsonic-response":{"status":"ok","version":"1.16.1","playlist":{
                  "id":"p1","name":"Mix","songCount":2,"owner":"alice",
                  "entry":[
                    {"id":"s1","title":"One","artist":"A","album":"X","duration":10,"suffix":"mp3"},
                    {"id":"s2","title":"Two","artist":"B","album":"Y","duration":20,"suffix":"flac"}
                  ]}}}
                """.trimIndent(),
            ).build(),
        )

        val detail = api().playlist("p1")

        assertEquals("Mix", detail.playlist.name)
        assertEquals(listOf("s1", "s2"), detail.songs.map { it.id })
        assertEquals(20, detail.songs[1].duration)
    }

    @Test
    fun missingLyricsDoNotFailPlayback() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":70,"message":"not found"}}}""",
            ).build(),
        )

        assertEquals("", api().lyrics("s1"))
    }

    @Test
    fun lyricsLinesGetTimecodes() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """
                {"subsonic-response":{"status":"ok","version":"1.16.1","lyricsList":{"structuredLyrics":[
                  {"lang":"eng","synced":true,"line":[
                    {"start":1000,"value":"first line"},
                    {"start":61500,"value":"second line"}
                  ]}
                ]}}}
                """.trimIndent(),
            ).build(),
        )

        assertEquals("00:01.00 first line\n01:01.50 second line", api().lyrics("s1"))
    }

    @Test
    fun serverAddressIsNormalized() {
        assertEquals("https://music.example.com/", NavidromeAccount.parseServer("music.example.com").toString())
        assertEquals("https://music.example.com:4533/", NavidromeAccount.parseServer(" music.example.com:4533 ").toString())
        assertEquals("http://192.168.1.9:4533/", NavidromeAccount.parseServer("http://192.168.1.9:4533/").toString())
        assertEquals("https://music.example.com/navidrome", NavidromeAccount.parseServer("music.example.com/navidrome/").toString())
        assertNull(NavidromeAccount.parseServer(""))
    }

    @Test
    fun subpathServerKeepsPathPrefix() {
        val base = NavidromeAccount.parseServer("music.example.com/navidrome")!!
        val api = SubsonicApi(base, "alice", "secret")
        assertEquals("https://music.example.com/navidrome/rest/getCoverArt.view?id=a1", api.coverUrl("a1", 0))
    }

    @Test
    fun markerShowsServerAndUserButNotPassword() {
        val account = NavidromeAccount("https://music.example.com", "alice", "secret").normalized()
        val marker = account.marker()
        assertFalse(marker.contains("secret"))
        assertEquals("music.example.com" to "alice", NavidromeAccount.parseMarker(marker))
    }
}
