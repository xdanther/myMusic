package com.mytv.mymusic.ui.poster

import androidx.compose.ui.unit.dp
import com.mytv.mymusic.ui.ListeningFeedUi
import org.junit.Assert.assertEquals
import org.junit.Test

class PosterWallScreenTest {
    private val feed = ListeningFeedUi(modeId = "fm", name = "私人漫游", providerId = "netease", providerName = "网易云音乐", appending = false)

    @Test
    fun queueSubtitleCountsFromOne() {
        assertEquals("30 tracks · playing track 3", wallSubtitle(null, 30, 2))
        assertEquals("30 tracks", wallSubtitle(null, 30, -1))
    }

    @Test
    fun feedSubtitleLeavesOutTheTotal() {
        assertEquals("Listening mode · 网易云音乐 · heard 12 tracks · 5 left", wallSubtitle(feed, 18, 12))
        assertEquals(
            "Listening mode · 网易云音乐 · heard 0 tracks · 14 left · loading more…",
            wallSubtitle(feed.copy(appending = true), 15, 0),
        )
    }

    @Test
    fun insetsFollowThePrototype() {
        // 手机：状态栏 28 + 8 + 48 = 84，导航栏 0 + 92
        assertEquals(84.dp to 92.dp, wallInsets(WallMetrics.Phone, 28.dp, 0.dp, 800))
        assertEquals(0.dp to 0.dp, wallInsets(WallMetrics.Tablet, 24.dp, 48.dp, 800))
        assertEquals(80.dp to 0.dp, wallInsets(WallMetrics.Tv, 0.dp, 0.dp, 540))
        // 横屏手机让完放不下大卡：都不让
        assertEquals(0.dp to 0.dp, wallInsets(WallMetrics.Phone, 24.dp, 0.dp, 360))
        // 桌面姿态胶囊挪到下半屏：顶上只让状态栏 28 + 8
        assertEquals(36.dp to 92.dp, wallInsets(WallMetrics.Phone, 28.dp, 0.dp, 841, pillOnTop = false))
        // 平板 / 电视不受影响
        assertEquals(0.dp to 0.dp, wallInsets(WallMetrics.Tablet, 24.dp, 48.dp, 800, pillOnTop = false))
    }
}
