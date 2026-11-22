package com.mytv.mymusic.widget

import com.mytv.mymusic.data.ListeningMode
import com.mytv.mymusic.data.Song

/** 组件选择器预览（Android 15 起实时生成）用的示例数据：不读用户的播放记录，封面显示占位。 */
object WidgetSamples {
    private val songs = listOf(
        Song(id = "1", name = "May We All Be Blessed", artist = "Faye Wong", source = "sample", duration = 283),
        Song(id = "2", name = "Brightest Star in the Night Sky", artist = "Escape Plan", source = "sample", duration = 252),
        Song(id = "3", name = "Later", artist = "Rene Liu", source = "sample", duration = 341),
        Song(id = "4", name = "Sunny Day", artist = "Jay Chou", source = "sample", duration = 269),
        Song(id = "5", name = "The Ordinary Road", artist = "Pu Shu", source = "sample", duration = 301),
        Song(id = "6", name = "A Little Happiness", artist = "Hebe Tien", source = "sample", duration = 265),
    )

    val list: List<Song> get() = songs

    private const val LYRICS = """[00:00.00]May We All Be Blessed
[02:00.00]明月几时有
[02:06.00]把酒问青天
[02:12.00]不知天上宫阙
[02:18.00]今夕是何年
[02:24.00]我欲乘风归去"""

    val playback: PlaybackSnapshot
        get() = PlaybackSnapshot(
            song = songs[0],
            index = 0,
            queueSize = songs.size,
            playing = true,
            live = true,
            positionMs = 128_000L,
            anchorAt = Long.MAX_VALUE,
            durationMs = 283_000L,
            lyrics = LYRICS,
            lyricsReady = true,
            sourceName = "NetEase",
            quality = "Lossless",
            hasPrevious = true,
            hasNext = true,
        )

    val mode = ListeningMode(id = "sample", name = "Default mode", subtitle = "Recommended for your taste", source = "netease", group = "Personal roam")

    val chart: ChartSnapshot
        get() = ChartSnapshot(source = "sample", id = "sample", name = "Trending", sourceName = "NetEase", songs = songs, updatedAt = System.currentTimeMillis())
}
