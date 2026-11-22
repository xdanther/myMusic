package com.mytv.mymusic.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mytv.mymusic.data.CoverShapeChoice
import com.mytv.mymusic.data.LyricSubline
import com.mytv.mymusic.data.LyricsBackgroundMode
import com.mytv.mymusic.data.MiniPlayerTarget
import com.mytv.mymusic.data.RHYTHM_OFFSET_MAX_MS
import com.mytv.mymusic.data.RhythmIntensity
import com.mytv.mymusic.data.TabletopTop
import com.mytv.mymusic.lyricon.LyriconSubscriptionState
import com.mytv.mymusic.overlay.FloatingLyricsService
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.MusicUiState
import com.mytv.mymusic.ui.components.ConnectedToggleGroup
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.SystemScriptConverter
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SegmentedGroup
import com.mytv.mymusic.ui.components.SwitchRow
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.player.LYRIC_FONT_FALLBACK
import com.mytv.mymusic.ui.player.LYRIC_FONT_PRIMARY
import com.mytv.mymusic.ui.player.label
import com.mytv.mymusic.ui.player.polygon
import com.mytv.mymusic.ui.player.rhythmDebugAvailable
import com.mytv.mymusic.ui.player.stage.AnimationSheet
import com.mytv.mymusic.ui.player.stage.label
import com.mytv.mymusic.ui.player.stage.modeOf
import com.mytv.mymusic.ui.player.stage.rememberAnimationDraft
import com.mytv.mymusic.ui.theme.PlayerTheme
import com.mytv.mymusic.ui.theme.isTelevision

internal val LyricsBackgroundMode.label: String
    get() = when (this) {
        LyricsBackgroundMode.Off -> "Off"
        LyricsBackgroundMode.Ambient -> "Dynamic"
        LyricsBackgroundMode.Reactive -> "Rhythm"
    }

internal val RhythmIntensity.label: String
    get() = when (this) {
        RhythmIntensity.Soft -> "Soft"
        RhythmIntensity.Standard -> "Standard"
        RhythmIntensity.Strong -> "Strong"
    }

internal val TabletopTop.label: String
    get() = when (this) {
        TabletopTop.Lyrics -> "Lyrics"
        TabletopTop.Cover -> "Cover"
        TabletopTop.Clock -> "Clock"
    }

internal val LyricSubline.label: String
    get() = when (this) {
        LyricSubline.Translation -> "Translation"
        LyricSubline.Romanization -> "Romanization"
        LyricSubline.Both -> "Show both"
    }

internal val MiniPlayerTarget.label: String
    get() = when (this) {
        MiniPlayerTarget.Player -> "Player"
        MiniPlayerTarget.PosterWall -> "Poster wall"
    }

/**
 * 设置 → 播放页（枚举仍叫 Lyrics）：Cover shape、歌词背景、Lyric animation和Lyric text、系统媒体条歌词、Rhythm strength和Beat sync calibration、
 * Tap the mini player to open什么；折叠屏展开时加桌面姿态上半屏放什么，电视上再加环境模式时钟。都只存在本机，改了立即生效。
 */
@Composable
internal fun LyricsSettingsScreen(
    state: MusicUiState,
    actions: MusicActions,
    lyricon: LyriconSubscriptionState,
    onBack: () -> Unit,
    embedded: Boolean = false,
) {
    val window = LocalAppWindow.current
    val mode = state.lyricsBackground
    val debuggable = rhythmDebugAvailable()
    var animationOpen by rememberSaveable { mutableStateOf(false) }
    val animationDraft = rememberAnimationDraft(animationOpen, state.lyricStage)
    val context = LocalContext.current
    val television = remember(context) { context.isTelevision() }
    var overlayGranted by remember { mutableStateOf(FloatingLyricsService.hasPermission(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = FloatingLyricsService.hasPermission(context)
    }
    val overlayPermissionPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayGranted = FloatingLyricsService.hasPermission(context)
        actions.setFloatingLyricsEnabled(true)
    }
    val primaryFontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.importLyricFont(LYRIC_FONT_PRIMARY, uri)
    }
    val fallbackFontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.importLyricFont(LYRIC_FONT_FALLBACK, uri)
    }
    if (animationDraft != null) {
        // 和全屏歌词页右上角 ✦ 打开的是同一个面板，套播放页的深色配色；这里没有“这首歌”和舞台，设置区顶上放大预览
        PlayerTheme {
            AnimationSheet(
                saved = state.lyricStage,
                draft = animationDraft,
                song = state.current,
                songRow = null,
                stageBeside = false,
                actions = actions,
                onApplied = {},
                onDismiss = { animationOpen = false },
            )
        }
    }
    val stage = state.lyricStage
    val text = state.lyricText
    SettingsPage(SettingsSection.Lyrics, embedded, onBack) {
        item(key = "shapeHead", contentType = "sectionHeader") { SectionLabel("Cover shape") }
        item(key = "shapes", contentType = "coverShapes") {
            CoverShapePicker(
                selected = state.coverShape,
                cover = state.current?.cover.orEmpty(),
                tv = window.isTv,
                onSelect = actions::setCoverShape,
            )
        }
        item(key = "shapeNote", contentType = "note") {
            SettingsNote("Random: a new shape on each track, never repeating the previous one. Picking a shape uses it for every track. All shapes become rounded squares when paused.")
        }
        item(key = "backgroundHead", contentType = "sectionHeader") { SectionLabel("Show lyric backdrop") }
        item(key = "background", contentType = "lyricsOptions") {
            Column {
                ConnectedToggleGroup(
                    options = LyricsBackgroundMode.entries,
                    selected = mode,
                    onSelect = actions::setLyricsBackground,
                    label = { it.label },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
                SettingsNote(
                    when (mode) {
                        LyricsBackgroundMode.Off -> "The player uses a solid background; cover and lyrics are unaffected."
                        LyricsBackgroundMode.Ambient -> "Glows tinted by the cover drift slowly in the background without reacting to the music."
                        LyricsBackgroundMode.Reactive -> "Section intensity changes the speed and amount of light; when the beat is detected the glow, cover and trails pulse in time with what you hear. The screen never flashes white on the beat."
                    },
                )
            }
        }
        item(key = "stageHead", contentType = "sectionHeader") { SectionLabel("Lyric animation") }
        item(key = "stage", contentType = "lyricStage") {
            SegmentedGroup {
                ListRow(
                    title = "Full-screen lyric animation",
                    index = 0,
                    count = 3,
                    onClick = { animationOpen = true },
                    summary = "Only applies to the full-screen lyric page",
                    value = stage.modeOf(stage.visualizer).label,
                )
                ListRow(
                    title = "Change animation per track",
                    index = 1,
                    count = 3,
                    onClick = { animationOpen = true },
                    value = stage.auto.label,
                )
                SwitchRow(
                    title = "Spectrum",
                    checked = stage.spectrum,
                    onCheckedChange = { on -> actions.setLyricStage { it.copy(spectrum = on) } },
                    index = 2,
                    count = 3,
                    summary = "Bottom-left of the full-screen lyric page",
                )
            }
        }
        item(key = "stageNote", contentType = "note") {
            SettingsNote("The ✦ in the top-right of the full-screen lyric page opens this same panel.")
        }
        item(key = "textHead", contentType = "sectionHeader") { SectionLabel("Lyric text") }
        item(key = "text", contentType = "lyricText") {
            SegmentedGroup {
                SwitchRow(
                    title = "Split bilingual lines",
                    checked = text.splitBilingual,
                    onCheckedChange = { on -> actions.setLyricText { it.copy(splitBilingual = on) } },
                    index = 0,
                    count = 3,
                    summary = "Splits lyrics that put the original and translation on the same line",
                )
                SwitchRow(
                    title = "Simplified to traditional",
                    checked = text.traditional && SystemScriptConverter.traditionalAvailable,
                    onCheckedChange = { on -> actions.setLyricText { it.copy(traditional = on) } },
                    index = 1,
                    count = 3,
                    summary = if (SystemScriptConverter.traditionalAvailable) "Converts character by character; a few may be inaccurate" else "Requires Android 10 or later",
                    enabled = SystemScriptConverter.traditionalAvailable,
                )
                SwitchRow(
                    title = "Transliterate when there is no romanization",
                    checked = text.autoRomanize && SystemScriptConverter.romanizeAvailable,
                    onCheckedChange = { on -> actions.setLyricText { it.copy(autoRomanize = on) } },
                    index = 2,
                    count = 3,
                    summary = if (SystemScriptConverter.romanizeAvailable) "Converts only Korean, kana and Cyrillic; Chinese characters are kept" else "Requires Android 10 or later",
                    enabled = SystemScriptConverter.romanizeAvailable,
                )
            }
        }
        item(key = "fontHead", contentType = "sectionHeader") { SectionLabel("Lyric font") }
        item(key = "fonts", contentType = "lyricFonts") {
            SegmentedGroup {
                ListRow(
                    title = "Preferred font",
                    index = 0,
                    count = 2,
                    onClick = { primaryFontPicker.launch(arrayOf("*/*")) },
                    summary = "The font used for Chinese, English or your main language",
                    value = state.lyricFonts.primaryName.ifBlank { "System default" },
                    trailing = if (state.lyricFonts.primaryName.isBlank()) null else {
                        { TextButton(onClick = { actions.clearLyricFont(LYRIC_FONT_PRIMARY) }) { Text("Remove") } }
                    },
                )
                ListRow(
                    title = "Fallback font for missing glyphs",
                    index = 1,
                    count = 2,
                    onClick = { fallbackFontPicker.launch(arrayOf("*/*")) },
                    summary = if (state.lyricFonts.primaryName.isBlank()) "The preferred font is used first; this font covers missing glyphs, then the system font" else "When the preferred font lacks glyphs this font is tried next, then the system font",
                    value = state.lyricFonts.fallbackName.ifBlank { "Not set" },
                    trailing = if (state.lyricFonts.fallbackName.isBlank()) null else {
                        { TextButton(onClick = { actions.clearLyricFont(LYRIC_FONT_FALLBACK) }) { Text("Remove") } }
                    },
                )
            }
        }
        item(key = "fontNote", contentType = "note") {
            SettingsNote("Supports TTF, OTF, TTC and OTC up to 30 MB each; fonts stay on this device and are used for player lyrics and animation previews. Android 10 and later fully support per-glyph fallback; older versions use only the preferred font for some animations.")
        }
        item(key = "sublineHead", contentType = "sectionHeader") { SectionLabel("The line under the lyrics") }
        item(key = "subline", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = LyricSubline.entries,
                selected = stage.subline,
                onSelect = { value -> actions.setLyricStage { it.copy(subline = value) } },
                label = { it.label },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            )
        }
        item(key = "textNote", contentType = "note") {
            SettingsNote("The first three apply to the full-screen lyric page, the system media bar, home-screen widgets, the poster wall and Lyricon; the last one only affects the player page and Lyricon. When lyrics have no romanization, choosing Romanization shows the translation.")
        }
        item(key = "notificationHead", contentType = "sectionHeader") { SectionLabel("System media bar") }
        item(key = "notification", contentType = "switch") {
            SwitchRow(
                title = "Show current lyric",
                checked = state.notificationLyrics,
                onCheckedChange = actions::setNotificationLyrics,
                index = 0,
                count = 1,
                summary = "Shows the current lyric line as the second line of the system notification and lock-screen media bar instead of the artist; the artist returns when lyrics are out of sync.",
            )
        }
        if (!television) {
            item(key = "floatingHead", contentType = "sectionHeader") { SectionLabel("Desktop lyrics (overlay)") }
            item(key = "floatingLyrics", contentType = "switch") {
                SwitchRow(
                    title = "Show desktop lyrics",
                    checked = state.floatingLyricsEnabled && overlayGranted,
                    onCheckedChange = { enabled ->
                        if (enabled && !overlayGranted) {
                            runCatching { overlayPermissionPicker.launch(FloatingLyricsService.permissionIntent(context)) }
                                .onFailure { actions.setFloatingLyricsEnabled(true) }
                        } else {
                            actions.setFloatingLyricsEnabled(enabled)
                        }
                    },
                    index = 0,
                    count = 1,
                    summary = if (overlayGranted) {
                        "Lyrics are centred in a transparent area 90% of the screen width and buttons auto-hide; tap to show play/pause, next and close."
                    } else {
                        "Requires the display over other apps permission; enabling jumps to the system permission page first."
                    },
                )
            }
            item(key = "floatingNote", contentType = "note") {
                SettingsNote("Tap the lyrics to show buttons, tap again to return to myMusic; buttons hide after a few seconds. Drag to move and the position is remembered; revoking the system permission hides the lyrics.")
            }
        }
        item(key = "lyriconHead", contentType = "sectionHeader") { SectionLabel("Lyricon") }
        item(key = "lyriconSwitches", contentType = "lyriconSwitches") {
            SegmentedGroup {
                SwitchRow(
                    title = "Send lyrics to Lyricon",
                    checked = state.lyriconProviderEnabled && lyricon.supported,
                    onCheckedChange = actions::setLyriconProviderEnabled,
                    index = 0,
                    count = 2,
                    summary = if (lyricon.supported) "Sends the current track, line and word lyrics, translations, romanization and playback progress." else "Requires Android 8.1 or later",
                    enabled = lyricon.supported,
                )
                SwitchRow(
                    title = "Receive lyrics from other players",
                    checked = state.lyriconSubscriberEnabled && lyricon.supported,
                    onCheckedChange = actions::setLyriconSubscriberEnabled,
                    index = 1,
                    count = 2,
                    summary = if (lyricon.supported) "Previews the song and lyric line of the player active in Lyricon below." else "Requires Android 8.1 or later",
                    enabled = lyricon.supported,
                )
            }
        }
        if (state.lyriconSubscriberEnabled && lyricon.supported) {
            item(key = "lyriconPreview", contentType = "lyriconPreview") { LyriconPreviewCard(lyricon) }
        }
        item(key = "lyriconNote", contentType = "note") {
            SettingsNote("Requires the Lyricon core service installed and active. Sending is on by default, receiving off; failing to reach Lyricon doesn't affect in-app lyrics or playback.")
        }
        item(key = "metadataHead", contentType = "sectionHeader") { SectionLabel("Lyrics and cover filling") }
        item(key = "metadataFallback", contentType = "switch") {
            SwitchRow(
                title = "Public metadata filling",
                checked = state.metadataFallback,
                onCheckedChange = actions::setMetadataFallback,
                index = 0,
                count = 1,
                summary = "When a platform has no lyrics or the track lacks a cover, lyrics are matched on LRCLIB and covers on Apple's public catalog by title, artist, album and duration. When on, that track information is sent to those services; weak matches are left empty.",
            )
        }
        item(key = "metadataNote", contentType = "note") {
            SettingsNote("Only fills gaps and never overwrites platform lyrics or embedded covers. The public catalog is strongest for English songs; new domestic releases may have no results.")
        }
        item(key = "appleLyricHead", contentType = "sectionHeader") { SectionLabel("Apple Music lyrics") }
        item(key = "appleLyricProxy", contentType = "switch") {
            SwitchRow(
                title = "Use third-party lyric relay",
                checked = state.thirdPartyLyrics,
                onCheckedChange = actions::setThirdPartyLyrics,
                index = 0,
                count = 1,
                summary = "Apple Music no longer provides lyrics to signed-out users. When on, community services such as zarz.moe, paxsenix and AMLL fetch word-by-word lyrics instead; requests send the Apple Music track ID to those services. When off, only Apple's official API is used and signed-out users get no lyrics.",
            )
        }
        item(key = "appleLyricNote", contentType = "note") {
            SettingsNote("Three sources are tried in order; if all fail the track shows no lyrics. These are community-run public services that can be overloaded or go offline at any time.")
        }
        // 强度只对“律动”有意义；开启“减少动画”时背景回退成静止，但选项照常显示
        if (mode == LyricsBackgroundMode.Reactive) {
            item(key = "intensityHead", contentType = "sectionHeader") { SectionLabel("Rhythm strength") }
            item(key = "intensity", contentType = "toggleGroup") {
                ConnectedToggleGroup(
                    options = RhythmIntensity.entries,
                    selected = state.rhythmIntensity,
                    onSelect = actions::setRhythmIntensity,
                    label = { it.label },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
            }
            item(key = "intensityNote", contentType = "note") {
                SettingsNote("Soft only pulses the glow and light trails. Standard adds a cover bounce and a ring of light from the cover outline on the first beat of each bar. Strong bounces harder and rings on every beat.")
            }
            item(key = "offsetHead", contentType = "sectionHeader") { SectionLabel("Beat sync calibration") }
            item(key = "offset", contentType = "rhythmOffset") { RhythmOffsetRow(state.rhythmOffsetMs, actions::setRhythmOffset) }
            item(key = "offsetNote", contentType = "note") {
                SettingsNote("Adjust here when the rhythm always runs slightly ahead of or behind the beat (common with Bluetooth headphones and speakers): positive delays the visuals, negative advances them. This device only.")
            }
            if (debuggable) {
                item(key = "rhythmDebug", contentType = "switch") {
                    SwitchRow(
                        title = "Show beat sync diagnostics",
                        checked = state.rhythmDebug,
                        onCheckedChange = actions::setRhythmDebug,
                        index = 0,
                        count = 1,
                        summary = "Shows tempo, confidence and analysis lead in the player's top-left corner, flashing on each beat",
                    )
                }
            }
            item(key = "reactiveNote", contentType = "note") {
                SettingsNote(
                    "Rhythm analyses the audio playing on this device; it needs no microphone permission and never stores or uploads audio." +
                        "Analysis stops on pause, on leaving the player or when Reduce animations is on; after resuming it adapts to the track's dynamic range again.",
                )
            }
        }
        item(key = "miniTargetHead", contentType = "sectionHeader") { SectionLabel("Tap the mini player to open") }
        item(key = "miniTarget", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = MiniPlayerTarget.entries,
                selected = state.miniPlayerTarget,
                onSelect = actions::setMiniPlayerTarget,
                label = { it.label },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            )
        }
        item(key = "miniTargetNote", contentType = "note") {
            SettingsNote(
                when {
                    window.isTv -> "Which page the Now playing bar at the top opens. The poster wall lays the queue out as a wall of covers with the playing track enlarged and its lyrics shown; Open player there switches at any time."
                    window.isPhone -> "The poster wall lays the queue out as a wall of covers with the playing track enlarged and its lyrics shown. With the poster wall chosen, swiping up the mini player still opens the player."
                    else -> "Which page opens when you tap the cover and title on the bottom bar or the cover in the player pane. The poster wall lays the queue out as a wall of covers with the playing track enlarged and its lyrics shown; Open player and Full screen are unaffected."
                },
            )
        }
        // 只在窗口横跨折痕（折叠屏内屏展开、双屏横跨）时才有意义，外屏、分屏里不显示
        if (window.fold != null && !window.isTv) {
            item(key = "tabletopHead", contentType = "sectionHeader") { SectionLabel("Foldable tabletop posture") }
            item(key = "tabletop", contentType = "toggleGroup") {
                ConnectedToggleGroup(
                    options = TabletopTop.entries,
                    selected = state.tabletopTop,
                    onSelect = actions::setTabletopTop,
                    label = { it.label },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
            }
            item(key = "tabletopNote", contentType = "note") {
                SettingsNote("What fills the top half of the player when the screen is half folded like a stand; the bottom half always holds progress and controls and never hides. Only the open player switches when you fold; other pages keep browsing.")
            }
        }
        // 强制电视布局也算电视：环境模式只在电视播放页出现
        if (window.isTv) {
            item(key = "tvHead", contentType = "sectionHeader") { SectionLabel("TV") }
            item(key = "tvClock", contentType = "switch") {
                SwitchRow(
                    title = "Show clock in ambient mode",
                    checked = state.tvAmbientClock,
                    onCheckedChange = actions::setTvAmbientClock,
                    index = 0,
                    count = 2,
                    summary = "Shows the time in the top-right once ambient mode starts after 4 idle seconds (follows the system 12/24-hour setting)",
                )
            }
            item(key = "keepScreenOn", contentType = "switch") {
                SwitchRow(
                    title = "Block screensaver during playback",
                    checked = state.keepScreenOn,
                    onCheckedChange = actions::setKeepScreenOn,
                    index = 1,
                    count = 2,
                    summary = "Keeps the screen on during playback so the system screensaver doesn't start; OLED screens risk burn-in, and listening with the screen off or leaving the app is unaffected",
                )
            }
            item(key = "keepScreenOnNote", contentType = "note") {
                SettingsNote("The system screensaver starts on screen timeout, so only keeping the screen on blocks it. Third-party screensavers launched by some TVs can't be blocked; music keeps playing in that case.")
            }
            item(key = "tvBackgroundPlay", contentType = "switch") {
                SwitchRow(
                    title = "Background playback",
                    checked = state.tvBackgroundPlay,
                    onCheckedChange = actions::setTvBackgroundPlay,
                    index = 2,
                    count = 3,
                    summary = "Music keeps playing after leaving the app: pressing Home, switching apps or exiting keeps it playing, just like on phones",
                )
            }
            item(key = "tvBackgroundPlayNote", contentType = "note") {
                SettingsNote("To stop, pause in the app or use stop in the notification or system media controls. A few TV systems kill background apps, and then playback does stop.")
            }
        }
    }
}

/**
 * Beat sync calibration：−200..+200ms，一次 10ms。用两个按钮而不是滑块：电视遥控器上一下一格，手机上也能点到精确的值。
 * 到头了按钮也不禁用（只是不再变），电视上焦点不会因为按钮变灰而跳走。
 */
@Composable
private fun RhythmOffsetRow(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(segmentShape(0, 1))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalIconButton(
            onClick = { onChange((value - RHYTHM_OFFSET_STEP_MS).coerceAtLeast(-RHYTHM_OFFSET_MAX_MS)) },
            modifier = Modifier.tvFocusRing(CircleShape),
        ) { Icon(Icons.Filled.Remove, contentDescription = "Advance visuals by 10 ms") }
        Text(
            rhythmOffsetLabel(value),
            Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + RHYTHM_OFFSET_STEP_MS).coerceAtMost(RHYTHM_OFFSET_MAX_MS)) },
            modifier = Modifier.tvFocusRing(CircleShape),
        ) { Icon(Icons.Filled.Add, contentDescription = "Delay visuals by 10 ms") }
        TextButton(onClick = { onChange(0) }, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text("Reset") }
    }
}

internal fun rhythmOffsetLabel(ms: Int): String = when {
    ms > 0 -> "Visuals $ms ms late"
    ms < 0 -> "Visuals ${-ms} ms early"
    else -> "No adjustment"
}

private const val RHYTHM_OFFSET_STEP_MS = 10

/**
 * 形状选择器：手机和平板是卡片里的 4 列网格，电视一行 7 个（窄窗口的强制电视布局会自动换行）。
 * 预览直接用当前播放的封面裁成对应形状，没在播放时用 primaryContainer 填色。
 * 电视右栏只有约 496dp，预览缩到 54dp、间距 2dp，7 个正好排进一行（488dp）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverShapePicker(
    selected: CoverShapeChoice,
    cover: String,
    tv: Boolean,
    onSelect: (CoverShapeChoice) -> Unit,
) {
    val context = LocalContext.current
    val previewSize = if (tv) 54.dp else 56.dp
    val px = with(LocalDensity.current) { previewSize.roundToPx() }
    // 6 个预览共用一个小尺寸请求，内存缓存里只有一份
    val request = remember(context, cover, px) {
        cover.takeIf { it.isNotBlank() }?.let { ImageRequest.Builder(context).data(it).size(px).build() }
    }
    val option: @Composable (CoverShapeChoice) -> Unit = { choice ->
        CoverShapeOption(choice, choice == selected, previewSize, request) { onSelect(choice) }
    }
    if (tv) {
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CoverShapeChoice.entries.forEach { option(it) }
        }
        return
    }
    Column(
        Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(segmentShape(0, 1))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .selectableGroup(),
    ) {
        CoverShapeChoice.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { choice ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { option(choice) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 单个选项：形状预览 + 名字。选中时外面 4dp 处画一圈 2.5dp 主色描边，右上角挂对勾。
 * 内边距（左右 7dp、上下 8dp）刚好容下描边和对勾，电视焦点环画在整个选项外面，和选中描边分得开。
 * 对勾往右上探出 4dp：再多就会被选项 20dp 的圆角裁掉一角（6dp 时约裁 2dp），加大内边距又会让电视一行排不下 7 个。
 */
@Composable
private fun CoverShapeOption(
    choice: CoverShapeChoice,
    selected: Boolean,
    previewSize: Dp,
    cover: ImageRequest?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val optionShape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .tvFocusRing(optionShape)
            .clip(optionShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(previewSize)
                .drawBehind {
                    if (selected) {
                        val stroke = 2.5.dp.toPx()
                        drawCircle(
                            scheme.primary,
                            radius = size.minDimension / 2 + 4.dp.toPx() + stroke / 2,
                            style = Stroke(stroke),
                        )
                    }
                },
        ) {
            ShapePreview(choice, cover)
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = BadgeOverhang, y = -BadgeOverhang)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = scheme.onPrimary)
                }
            }
        }
        Text(
            choice.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** “随机”画成九瓣饼干加随机图标；具体形状裁当前封面，图还没加载出来时露出底色。 */
@Composable
private fun ShapePreview(choice: CoverShapeChoice, cover: ImageRequest?) {
    val scheme = MaterialTheme.colorScheme
    val polygon = choice.polygon
    Box(
        Modifier
            .fillMaxSize()
            .clip((polygon ?: MaterialShapes.Cookie9Sided).toShape())
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            polygon == null -> Icon(Icons.Filled.Shuffle, null, Modifier.size(26.dp), tint = scheme.onPrimaryContainer)
            cover != null -> AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private val BadgeOverhang = 4.dp
