package com.mytv.mymusic.ui.player

import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import com.mytv.mymusic.data.CoverShapeChoice

/** 设置里显示的名字。 */
val CoverShapeChoice.label: String
    get() = when (this) {
        CoverShapeChoice.Random -> "Shuffle"
        CoverShapeChoice.Cookie12 -> "Twelve-sided"
        CoverShapeChoice.Cookie9 -> "Nine-sided"
        CoverShapeChoice.Cookie7 -> "Seven-sided"
        CoverShapeChoice.Sunny -> "Sun"
        CoverShapeChoice.SoftBurst -> "Starburst"
        CoverShapeChoice.Clover8 -> "Eight-leaf"
    }

/** 对应的 MaterialShapes 原形；“随机”没有固定形状，切歌时由播放层抽取（[com.mytv.mymusic.playback.NowPlayingShape]）。 */
val CoverShapeChoice.polygon: RoundedPolygon?
    get() = when (this) {
        CoverShapeChoice.Random -> null
        CoverShapeChoice.Cookie12 -> MaterialShapes.Cookie12Sided
        CoverShapeChoice.Cookie9 -> MaterialShapes.Cookie9Sided
        CoverShapeChoice.Cookie7 -> MaterialShapes.Cookie7Sided
        CoverShapeChoice.Sunny -> MaterialShapes.Sunny
        CoverShapeChoice.SoftBurst -> MaterialShapes.SoftBurst
        CoverShapeChoice.Clover8 -> MaterialShapes.Clover8Leaf
    }

/**
 * 暂停态统一用圆角方形看全图（形状会裁掉封面约一成的边角），不参与随机。
 * 用 getter 而不是初始化值：MaterialShapes 自己会缓存，也免得只用随机函数时提前构造多边形。
 */
val PausedCoverPolygon: RoundedPolygon get() = MaterialShapes.Square
