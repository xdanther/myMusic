/*
 * Copyright (C) 2026  Shubham Gorai
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// 2026-11-09 myMusic 改写：Rush 7.2.1 的歌词页（LyricsPage 里画歌词和背景的部分）做成 myMusic 的一种全屏歌词动画。
// 设置照 LyricsPrefs 取跟全屏歌词有关的 7 项：背景（六种拆成“背景”和“Rhythm style”两行）、配色（自定义取色器换成“播放页”）、
// 对齐、字号、Depth blur、Per-character weight；行高、字距保持 Rush 默认（32sp / 0）。页头、分享卡片、LRCLIB 纠错、播放键发光不搬。
// 2026-11-18 myMusic 改动：Depth blur从开关改成强度滑块（关 · 弱 · 中 · 强，0–1.5 倍），旧的开关值读成 0 或 1。
// 2026-11-18 myMusic 改动：配色加回“自定义”，选了以后多出“背景”“文字”两个色块（取色面板见 ColorPickerSheet）；
// 上面“自定义取色器换成播放页”那句不再成立，播放页配色保留。
package com.mytv.mymusic.ui.player.stage.modes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mytv.mymusic.ui.player.stage.LyricSetting
import com.mytv.mymusic.ui.player.stage.LyricStageContext
import com.mytv.mymusic.ui.player.stage.LyricVisualizer
import com.mytv.mymusic.ui.player.stage.ModOrigin
import com.mytv.mymusic.ui.player.stage.SettingCondition
import com.mytv.mymusic.ui.player.stage.SettingValues
import com.mytv.mymusic.ui.player.stage.StageDevice
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rush：封面取色铺满整页，左对齐的粗体大字，当前句停在安全区上三分之一处，前后的句子缩到 80% 并按距离模糊，
 * 正在唱的那句逐字羽化扫亮，间奏三个点。可拖动、点一句跳过去；背景六种，“律动”三种跟着音乐动。
 */
object RushVisualizer : LyricVisualizer {
    override val id = "rush"
    override val label = "Rush"
    override val origin = ModOrigin("Rush", "7.2.1", "shub39", "GPL-3.0", kind = "App")
    override val order = 1

    /** 行高、字距保持 Rush 默认。 */
    override val hiddenSettings = 2
    override val hostBackground = false
    override val hostSubtitles = false
    override val interactive = true

    /** Depth blur的默认值随设备（电视、低内存、Android 12 以下默认关），两份表各建一次，设置值的表身份不变。 */
    override val settings: List<LyricSetting>
        get() = if (StageDevice.liveBlur) SETTINGS_BLUR_ON else SETTINGS_BLUR_OFF

    /** 只有背景选“律动”时才要律动分析。 */
    override fun wantsAudio(settings: SettingValues): Boolean = settings.choice(RUSH_KEY_BACKGROUND) == RUSH_BG_RHYTHM

    @Composable
    override fun Background(ctx: LyricStageContext, modifier: Modifier) {
        val look = remember(ctx.settings) { rushLook(ctx.settings) }
        val palette = remember(ctx.colors, look.colors, look.customBackground, look.customText) { look.palette(ctx) }
        val spec = colorSpec(ctx)
        val colors = RushBackgroundColors(
            background = animateColorAsState(palette.background, spec, label = "rushBackground"),
            wave = animateColorAsState(palette.wave, spec, label = "rushWave"),
            hypnoticLight = animateColorAsState(palette.hypnotic.first(), spec, label = "rushHypnoticLight"),
            hypnoticDark = animateColorAsState(palette.hypnotic.last(), spec, label = "rushHypnoticDark"),
        )
        RushBackground(ctx, look, colors, modifier)
    }

    @Composable
    override fun Stage(ctx: LyricStageContext, modifier: Modifier) {
        val look = remember(ctx.settings) { rushLook(ctx.settings) }
        val palette = remember(ctx.colors, look.colors, look.customBackground, look.customText) { look.palette(ctx) }
        val content: State<Color> = animateColorAsState(palette.content, colorSpec(ctx), label = "rushContent")
        RushLyricList(ctx, look, content, modifier)
    }

    /** 换封面时底色和字色一起过渡；减少动画时直接换。 */
    @Composable
    private fun colorSpec(ctx: LyricStageContext): AnimationSpec<Color> =
        if (ctx.display.reduceMotion) snap() else MaterialTheme.motionScheme.defaultEffectsSpec()
}

/** 读当前设置。 */
internal fun rushLook(settings: SettingValues): RushLook = RushLook(
    background = settings.choice(RUSH_KEY_BACKGROUND),
    rhythm = settings.choice(RUSH_KEY_RHYTHM),
    colors = settings.choice(RUSH_KEY_COLORS),
    align = when (settings.choice(RUSH_KEY_ALIGN)) {
        RUSH_ALIGN_CENTER -> RushAlign.Center
        RUSH_ALIGN_END -> RushAlign.End
        else -> RushAlign.Start
    },
    size = settings.number(RUSH_KEY_SIZE),
    blur = settings.slider(RUSH_KEY_BLUR),
    expressive = settings.toggle(RUSH_KEY_EXPRESSIVE),
    customBackground = settings.color(RUSH_KEY_CUSTOM_BACKGROUND),
    customText = settings.color(RUSH_KEY_CUSTOM_TEXT),
)

private fun RushLook.palette(ctx: LyricStageContext): RushPalette = rushPalette(ctx.colors, colors, customBackground, customText)

private fun rushSettings(blurDefault: Boolean): List<LyricSetting> = listOf(
    LyricSetting.Choice(
        RUSH_KEY_BACKGROUND,
        "Background",
        listOf(RUSH_BG_SOLID to "Solid", RUSH_BG_COVER to "Cover", RUSH_BG_HYPNOTIC to "Light trails", RUSH_BG_RHYTHM to "Rhythm"),
        RUSH_BG_SOLID,
    ),
    LyricSetting.Choice(
        RUSH_KEY_RHYTHM,
        "Rhythm style",
        listOf(RUSH_RHYTHM_WAVE to "Waves", RUSH_RHYTHM_GRADIENT to "Gradient", RUSH_RHYTHM_CURVE to "Stripes"),
        RUSH_RHYTHM_WAVE,
        summary = "Applies when the Rhythm background is selected",
    ),
    LyricSetting.Choice(
        RUSH_KEY_COLORS,
        "Colors",
        listOf(RUSH_COLORS_VIBRANT to "Vivid", RUSH_COLORS_MUTED to "Soft", RUSH_COLORS_PLAYER to "Player", RUSH_COLORS_CUSTOM to "Custom"),
        RUSH_COLORS_VIBRANT,
        // 随机搭配不抽“自定义”：抽到了也只是沿用上次的两个颜色
        shuffleExclude = setOf(RUSH_COLORS_CUSTOM),
    ),
    // 切到“自定义”的那一下按封面的鲜艳色和它的文字色种初值，画面不跳
    LyricSetting.Color(
        RUSH_KEY_CUSTOM_BACKGROUND,
        "Background",
        RUSH_CUSTOM_BACKGROUND_HEX,
        seed = { it.coverVibrant },
        visibleWhen = SettingCondition(RUSH_KEY_COLORS, setOf(RUSH_COLORS_CUSTOM)),
    ),
    LyricSetting.Color(
        RUSH_KEY_CUSTOM_TEXT,
        "Text",
        RUSH_CUSTOM_TEXT_HEX,
        seed = { it.onCoverVibrant },
        visibleWhen = SettingCondition(RUSH_KEY_COLORS, setOf(RUSH_COLORS_CUSTOM)),
    ),
    LyricSetting.Choice(
        RUSH_KEY_ALIGN,
        "Alignment",
        listOf(RUSH_ALIGN_START to "Left", RUSH_ALIGN_CENTER to "Medium", RUSH_ALIGN_END to "Right"),
        RUSH_ALIGN_START,
    ),
    LyricSetting.Number(RUSH_KEY_SIZE, "Size", 0.8f, 1.4f, 0.1f, 1f),
    // “中”就是原来开着时的强度；电视、低内存、Android 12 以下默认关
    LyricSetting.Slider(
        RUSH_KEY_BLUR,
        "Depth blur",
        0f,
        RUSH_BLUR_DEPTH_MAX,
        default = if (blurDefault) 1f else 0f,
        ticks = listOf(0f, 0.5f, 1f, RUSH_BLUR_DEPTH_MAX),
        tickLabels = listOf("Off", "Light", "Medium", "Strong"),
        format = ::rushBlurLabel,
        legacyToggle = 0f to 1f,
        minSdk = 31,
        summary = "Lines further from the current one blur more, suspended while dragging lyrics; Android 12 and later",
    ),
    LyricSetting.Toggle(RUSH_KEY_EXPRESSIVE, "Per-character weight", false, summary = "Longer-sung characters get bolder; the font needs a weight axis"),
)

/**
 * 景深滑块的读数：“关”，或“中 · 每离一句多 3.0dp，最多 10dp”（[depth] 是强度倍数，见 [rushBlurDp]）。
 */
internal fun rushBlurLabel(depth: Float): String {
    if (depth <= RUSH_BLUR_OFF) return "Off"
    val level = when {
        depth < 0.75f -> "Light"
        depth < 1.25f -> "Medium"
        else -> "Strong"
    }
    val step = String.format(Locale.ROOT, "%.1f", RUSH_BLUR_STEP_DP * depth)
    return "$level · ${step}dp more per line away, up to ${(RUSH_BLUR_MAX_DP * depth).roundToInt()}dp"
}

private val SETTINGS_BLUR_ON = rushSettings(blurDefault = true)
private val SETTINGS_BLUR_OFF = rushSettings(blurDefault = false)

internal const val RUSH_KEY_BACKGROUND = "background"
internal const val RUSH_KEY_RHYTHM = "rhythm"
internal const val RUSH_KEY_COLORS = "colors"
internal const val RUSH_KEY_CUSTOM_BACKGROUND = "customBackground"
internal const val RUSH_KEY_CUSTOM_TEXT = "customText"
internal const val RUSH_KEY_ALIGN = "align"
internal const val RUSH_KEY_SIZE = "size"
internal const val RUSH_KEY_BLUR = "blur"
internal const val RUSH_KEY_EXPRESSIVE = "expressive"

/** 景深强度上限（“强”）：最多模糊 15dp。 */
internal const val RUSH_BLUR_DEPTH_MAX = 1.5f

/** 强度不到这个就算关。 */
internal const val RUSH_BLUR_OFF = 0.01f
private const val RUSH_ALIGN_START = "start"
private const val RUSH_ALIGN_CENTER = "center"
private const val RUSH_ALIGN_END = "end"
