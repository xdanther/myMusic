package com.mytv.mymusic.ui.settings

import android.os.Build
import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mytv.mymusic.R
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.mytv.mymusic.aggregate.MatchStrictness
import com.mytv.mymusic.aggregate.PlaybackMode
import com.mytv.mymusic.data.CrossfadeAnimation
import com.mytv.mymusic.data.CrossfadeMode
import com.mytv.mymusic.data.native.AudioQuality
import com.mytv.mymusic.data.ProviderInfo
import com.mytv.mymusic.data.lx.LxPlatform
import com.mytv.mymusic.data.lx.LxSources
import com.mytv.mymusic.data.playreport.PlayReportHub
import com.mytv.mymusic.ui.AccountProfile
import com.mytv.mymusic.ui.Cover
import com.mytv.mymusic.ui.adaptive.DialogAvoidsFold
import com.mytv.mymusic.ui.CoverBox
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.MusicChip
import com.mytv.mymusic.ui.MusicUiState
import com.mytv.mymusic.ui.TopTab
import com.mytv.mymusic.ui.components.AdaptiveSheet
import com.mytv.mymusic.ui.components.ConnectedToggleGroup
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.PlayButtonSize
import com.mytv.mymusic.ui.components.PlayPauseButton
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SegmentGap
import com.mytv.mymusic.ui.components.SegmentedGroup
import com.mytv.mymusic.ui.components.SkeletonRows
import com.mytv.mymusic.ui.components.SwitchRow
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.formatBytes
import com.mytv.mymusic.ui.providerName
import com.mytv.mymusic.ui.theme.BlurMode
import com.mytv.mymusic.ui.theme.BrandSeed
import com.mytv.mymusic.ui.theme.ColorSource
import com.mytv.mymusic.ui.theme.CustomSeedSwatches
import com.mytv.mymusic.ui.theme.FontScale
import com.mytv.mymusic.ui.theme.LayoutMode
import com.mytv.mymusic.ui.theme.LocalAppearance
import com.mytv.mymusic.ui.theme.SchemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

// ======================= 外观 =======================

private val CONTRAST_LEVELS = listOf(0f, 0.5f, 1f)

private fun contrastLabel(resources: Resources, value: Float): String = resources.getString(
    when {
        value >= 0.75f -> R.string.appearance_contrast_high
        value >= 0.25f -> R.string.appearance_contrast_medium
        else -> R.string.appearance_contrast_standard
    },
)

/**
 * 外观：预览卡片 + 主题 + 配色来源 + 风格 + 对比度 + 显示。
 * 配色来源手机两列、双栏一行四个；电视不提供纯黑背景（设计里电视固定深色）。
 */
@Composable
fun AppearanceSettingsScreen(
    state: MusicUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    embedded: Boolean = false,
) {
    val window = LocalAppWindow.current
    val tv = window.isTv
    val appearance = state.appearance
    val resources = LocalContext.current.resources
    val groupWidth = Modifier.widthIn(max = 480.dp).fillMaxWidth()
    SettingsPage(SettingsSection.Appearance, embedded, onBack) {
        item(key = "preview", contentType = "appearancePreview") { AppearancePreview(state, actions) }
        item(key = "layoutHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_layout_mode))
        }
        item(key = "layout", contentType = "layoutOptions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutModeChips(appearance.layout) { actions.setLayoutMode(it) }
                SettingsNote(layoutModeHint(resources, appearance.layout))
            }
        }
        item(key = "startupHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_start_page))
        }
        item(key = "startup", contentType = "startupOptions") {
            StartupPageOptions(state.startupTab) { actions.setStartupTab(it) }
        }
        item(key = "fontHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_text_size))
        }
        item(key = "fontScale", contentType = "fontScaleOptions") {
            Column {
                ConnectedToggleGroup(
                    options = FontScale.entries,
                    selected = appearance.fontScale,
                    onSelect = { actions.setFontScale(it) },
                    label = { resources.getString(it.labelRes) },
                    modifier = groupWidth,
                )
                SettingsNote(
                    stringResource(if (tv) R.string.appearance_font_note_tv else R.string.appearance_font_note),
                )
            }
        }
        // 电视固定深色，不提供浅色主题
        if (!tv) item(key = "themeHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_theme))
        }
        if (!tv) item(key = "theme", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = listOf("system", "light", "dark"),
                selected = appearance.theme,
                onSelect = { actions.setTheme(it) },
                label = { themeLabel(resources, it) },
                modifier = groupWidth,
                icon = {
                    when (it) {
                        "light" -> Icons.Filled.LightMode
                        "dark" -> Icons.Filled.DarkMode
                        else -> Icons.Filled.BrightnessAuto
                    }
                },
            )
        }
        item(key = "sourceHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_color_source))
        }
        item(key = "source", contentType = "colorOptions") { ColorSourceOptions(state, actions, perRow = if (window.twoPane) 4 else 2) }
        if (appearance.colorSource == ColorSource.Custom) {
            item(key = "swatches", contentType = "swatches") {
                SeedSwatches(appearance.customSeed) { actions.setCustomSeed(it) }
            }
        }
        item(key = "styleHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_color_style))
        }
        item(key = "browseStyle", contentType = "styleChips") {
            StyleChips(stringResource(R.string.appearance_style_browse), appearance.browseStyle) { actions.setBrowseStyle(it) }
        }
        item(key = "playerStyle", contentType = "styleChips") {
            StyleChips(stringResource(R.string.appearance_style_player), appearance.playerStyle) { actions.setPlayerStyle(it) }
        }
        item(key = "contrastHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_contrast))
        }
        item(key = "contrast", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = CONTRAST_LEVELS,
                selected = CONTRAST_LEVELS.minBy { abs(it - appearance.contrast) },
                onSelect = { actions.setContrast(it) },
                label = { contrastLabel(resources, it) },
                modifier = groupWidth,
            )
        }
        item(key = "displayHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_display))
        }
        if (!tv) {
            item(key = "amoled", contentType = "switch") {
                SwitchRow(
                    title = stringResource(R.string.appearance_amoled),
                    checked = appearance.amoled,
                    onCheckedChange = { actions.setAmoled(it) },
                    index = 0,
                    count = 2,
                    summary = stringResource(R.string.appearance_amoled_summary),
                    icon = Icons.Filled.Contrast,
                )
            }
        }
        item(key = "motion", contentType = "switch") {
            SwitchRow(
                title = stringResource(R.string.appearance_reduce_motion),
                checked = appearance.reduceMotion,
                onCheckedChange = { actions.setReduceMotion(it) },
                index = if (tv) 0 else 1,
                count = if (tv) 1 else 2,
                summary = stringResource(R.string.appearance_reduce_motion_summary),
                icon = Icons.Filled.Animation,
            )
        }
        item(key = "blurHead", contentType = "sectionHeader") {
            SectionLabel(stringResource(R.string.appearance_blur))
        }
        item(key = "blur", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = BlurMode.entries,
                selected = appearance.blur,
                onSelect = { actions.setBlurMode(it) },
                label = { resources.getString(it.labelRes) },
                modifier = groupWidth,
            )
        }
        item(key = "blurNote", contentType = "note") {
            SettingsNote(
                stringResource(if (tv) R.string.appearance_blur_note_tv else R.string.appearance_blur_note),
            )
        }
    }
}

private fun layoutModeHint(resources: Resources, mode: LayoutMode): String = resources.getString(
    when (mode) {
        LayoutMode.Auto -> R.string.appearance_layout_hint_auto
        LayoutMode.Phone -> R.string.appearance_layout_hint_phone
        LayoutMode.Tablet -> R.string.appearance_layout_hint_tablet
        LayoutMode.Tv -> R.string.appearance_layout_hint_tv
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutModeChips(selected: LayoutMode, onSelect: (LayoutMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LayoutMode.entries.forEach { mode ->
            val label = stringResource(mode.labelRes)
            MusicChip(
                text = label,
                selected = mode == selected,
                description = stringResource(R.string.appearance_layout_desc, label),
            ) { onSelect(mode) }
        }
    }
}

/** 启动页：冷打开应用时先停在哪个顶层入口。设置里存的是路由，认不出来的回默认'Charts'。 */
@Composable
private fun StartupPageOptions(selectedRoute: String, onSelect: (String) -> Unit) {
    val selected = TopTab.entries.firstOrNull { it.route == selectedRoute } ?: TopTab.Charts
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopTab.entries.forEach { tab ->
                val label = stringResource(tab.labelRes)
                MusicChip(
                    text = label,
                    selected = tab == selected,
                    description = stringResource(R.string.appearance_start_desc, label),
                ) { onSelect(tab.route) }
            }
        }
        SettingsNote(stringResource(R.string.appearance_start_note))
    }
}

/** 预览卡片：直接用当前配色角色画，有歌在放时显示它的封面和播放键。 */
@Composable
private fun AppearancePreview(state: MusicUiState, actions: MusicActions) {
    val scheme = MaterialTheme.colorScheme
    val song = state.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (song != null) {
            Cover(song.cover, song.title, 56.dp, corner = 16.dp)
        } else {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Palette, null, tint = scheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                song?.title ?: "myMusic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.appearance_preview), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primaryContainer).forEach { color ->
                    Box(Modifier.size(20.dp).clip(CircleShape).background(color))
                }
            }
        }
        if (song != null) {
            PlayPauseButton(playing = state.playing, onClick = { actions.transport("play") }, size = PlayButtonSize.Small)
        }
    }
}

@Composable
private fun ColorSourceOptions(state: MusicUiState, actions: MusicActions, perRow: Int) {
    val appearance = state.appearance
    val resources = LocalContext.current.resources
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSource.entries.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { source ->
                    ColorSourceCard(
                        source = source,
                        selected = appearance.colorSource == source,
                        // 系统壁纸取色需要 Android 8.1（API 27）的 WallpaperColors
                        enabled = source != ColorSource.Wallpaper || Build.VERSION.SDK_INT >= 27,
                        hint = colorSourceHint(resources, source),
                        onClick = { actions.setColorSource(source) },
                        modifier = Modifier.weight(1f),
                    ) { sampleModifier -> ColorSourceSample(source, state, sampleModifier) }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun colorSourceHint(resources: Resources, source: ColorSource): String = when {
    source != ColorSource.Wallpaper -> resources.getString(source.hintRes)
    Build.VERSION.SDK_INT >= 31 -> resources.getString(R.string.appearance_wallpaper_accent)
    Build.VERSION.SDK_INT >= 27 -> resources.getString(R.string.appearance_wallpaper_color)
    else -> resources.getString(R.string.appearance_wallpaper_unsupported)
}

@Composable
private fun ColorSourceCard(
    source: ColorSource,
    selected: Boolean,
    enabled: Boolean,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sample: @Composable (Modifier) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = LocalAppearance.current.reduceMotion
    val corner = animateDpAsState(
        if (selected) 24.dp else 20.dp,
        if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "sourceCorner",
    )
    val ringShape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .tvFocusRing(ringShape)
            .graphicsLayer {
                shape = RoundedCornerShape(corner.value)
                clip = true
            }
            .background(if (selected) scheme.surfaceContainerHigh else scheme.surfaceContainer)
            .drawWithContent {
                drawContent()
                if (selected) {
                    drawOutline(
                        RoundedCornerShape(corner.value).createOutline(size, layoutDirection, this),
                        scheme.primary,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .alpha(if (enabled) 1f else 0.48f)
            .padding(10.dp),
    ) {
        sample(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)))
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(source.labelRes),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (selected) Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = scheme.primary)
        }
        // 色值（#006B5F）对读屏没有意义，名字已经说明是哪种颜色
        Text(
            hint,
            if (hint.startsWith("#")) Modifier.clearAndSetSemantics {} else Modifier,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorSourceSample(source: ColorSource, state: MusicUiState, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    when (source) {
        ColorSource.Cover -> {
            val song = state.current
            if (song != null && song.cover.isNotBlank()) {
                CoverBox(song.cover, song.title, modifier, RoundedCornerShape(12.dp))
            } else {
                Box(modifier.background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.secondaryContainer))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, tint = scheme.onPrimaryContainer)
                }
            }
        }
        ColorSource.Wallpaper -> Box(modifier.background(Brush.linearGradient(listOf(scheme.tertiaryContainer, scheme.secondaryContainer))))
        // 样本就是种子色本身
        ColorSource.Brand -> Box(modifier.background(BrandSeed))
        ColorSource.Custom -> Box(modifier.background(Brush.horizontalGradient(CustomSeedSwatches)))
    }
}

/** 自选颜色的色板。选中项色块缩小并加一圈描边，不只靠颜色区分。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeedSwatches(selected: Int, onSelect: (Int) -> Unit) {
    val ring = MaterialTheme.colorScheme.onSurface
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 12.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CustomSeedSwatches.forEachIndexed { index, color ->
            val argb = color.toArgb()
            val isSelected = argb == selected
            val swatchLabel = stringResource(R.string.appearance_swatch, index + 1)
            Box(
                Modifier
                    .size(48.dp)
                    .tvFocusRing(CircleShape)
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(argb) })
                    .semantics { contentDescription = swatchLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(if (isSelected) 30.dp else 38.dp).clip(CircleShape).background(color))
                if (isSelected) Box(Modifier.size(42.dp).border(3.dp, ring, CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleChips(label: String, selected: SchemeStyle, onSelect: (SchemeStyle) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.padding(start = 4.dp, top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SchemeStyle.entries.forEach { style ->
                val styleLabel = stringResource(style.labelRes)
                MusicChip(
                    text = styleLabel,
                    selected = style == selected,
                    description = stringResource(R.string.appearance_style_desc, label, styleLabel),
                ) { onSelect(style) }
            }
        }
    }
}

// ======================= 播放与换源 =======================

/** 音质偏好：常用档 + 洛雪源普遍声明的全景声/母带；锁了档取不到时换源层会自动退回自动档。 */
private val QUALITY_PREFERENCE_OPTIONS = listOf(
    AudioQuality.Auto,
    AudioQuality.Standard,
    AudioQuality.High,
    AudioQuality.Lossless,
    AudioQuality.HiRes,
    AudioQuality.Atmos,
    AudioQuality.Master,
)

/** 播放与换源：Source switching按钮组、平台顺序入口、匹配选项。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackSettingsScreen(
    state: MusicUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    embedded: Boolean = false,
    onOpen: (SettingsSection) -> Unit = {},
) {
    val aggregation = state.aggregation
    val searchable = state.providers.filter { it.search }
    val lxRecords by LxSources.records.collectAsState()
    val lxProvider = ProviderInfo(id = LxPlatform.PROVIDER_ID, name = LxPlatform.PROVIDER_NAME)
    val pinnable = searchable + listOfNotNull(lxProvider.takeIf { lxRecords.isNotEmpty() })
    val priority = aggregation.priorityFor(searchable.map { it.id } + LxPlatform.PROVIDER_ID)
    SettingsPage(SettingsSection.Playback, embedded, onBack) {
        item(key = "crossHead", contentType = "sectionHeader") { SectionLabel("Crossfade") }
        item(key = "crossSwitch", contentType = "switch") {
            SwitchRow(
                title = "Crossfade",
                checked = state.crossfadeEnabled,
                onCheckedChange = { actions.setCrossfadeEnabled(it) },
                index = 0,
                count = 1,
                summary = "Brings the next track in before the previous one ends; turn it off to play each track fully before switching",
                icon = Icons.Filled.AutoAwesome,
            )
        }
        if (state.crossfadeEnabled) {
            item(key = "crossModeHead", contentType = "sectionHeader") { SectionLabel("Mode") }
            item(key = "crossMode", contentType = "toggleGroup") {
                ConnectedToggleGroup(
                    options = CrossfadeMode.entries,
                    selected = state.crossfadeMode,
                    onSelect = { actions.setCrossfadeMode(it) },
                    label = { it.label },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
            }
            when (state.crossfadeMode) {
                CrossfadeMode.Fade -> {
                    item(key = "crossLengthHead", contentType = "sectionHeader") { SectionLabel("Crossfade duration") }
                    item(key = "crossLength", contentType = "toggleGroup") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConnectedToggleGroup(
                                options = listOf(4, 6, 8, 10),
                                selected = state.crossfadeSeconds,
                                onSelect = { actions.setCrossfadeSeconds(it) },
                                label = { "$it s" },
                                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                            )
                            SettingsNote("Fading happens in the last seconds of a track. Manual skips, live streams and DRM full tracks still switch directly.")
                        }
                    }
                }
                CrossfadeMode.Automix -> {
                    item(key = "crossAnimHead", contentType = "sectionHeader") { SectionLabel("Crossfade animation") }
                    item(key = "crossAnim", contentType = "toggleGroup") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConnectedToggleGroup(
                                options = CrossfadeAnimation.entries,
                                selected = state.crossfadeAnimation,
                                onSelect = { actions.setCrossfadeAnimation(it) },
                                label = { it.label },
                                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                            )
                            SettingsNote("Crossfades shorter than 5 seconds draw no animation; tap the ring on the player page to dismiss it.")
                        }
                    }
                    item(key = "crossCache", contentType = "settingRow") {
                        ListRow(
                            title = "Playback cache ${if (state.cacheLimitMb >= 1024) "${state.cacheLimitMb / 1024} GB" else "${state.cacheLimitMb} MB"}",
                            index = 0,
                            count = 1,
                            onClick = { onOpen(SettingsSection.Storage) },
                            icon = Icons.Filled.History,
                            summary = "Analysis must read both tracks fully; with too little cache it falls back to equal-power fading",
                        )
                    }
                }
                CrossfadeMode.None -> {
                    item(key = "crossNone", contentType = "infoNote") {
                        SettingsNote("Plays each track fully and then switches, same as turning the master switch off; the next track is not prepared in advance either.")
                    }
                }
            }
            state.crossfadeReport?.takeIf { state.crossfadeMode != CrossfadeMode.None }?.let { report ->
                item(key = "crossLast", contentType = "settingRow") {
                    ListRow(
                        title = report.headline,
                        index = 0,
                        count = 1,
                        onClick = null,
                        icon = Icons.Filled.AutoAwesome,
                        summary = report.detail,
                        showChevron = false,
                    )
                }
            }
        }
        item(key = "qualityHead", contentType = "sectionHeader") { SectionLabel("Playback quality") }
        item(key = "quality", contentType = "chips") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedQuality = AudioQuality.normalize(state.audioQuality) ?: AudioQuality.Auto
                // 七档塞不进一行连接按钮组，窄屏每格只剩几十 dp、字全被省略；chip 自动换行，宽度跟着文字走
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QUALITY_PREFERENCE_OPTIONS.forEach { quality ->
                        MusicChip(quality.preferenceLabel, quality == selectedQuality) { actions.setAudioQuality(quality) }
                    }
                }
                SettingsNote(
                    "A locked tier is the only one requested; when the platform can't provide it (not available, no membership) the app tries another platform and only then falls back to auto." +
                        "Available tiers depend on your account; Spotify and Apple Music only provide preview clips.",
                )
            }
        }
        item(key = "modeHead", contentType = "sectionHeader") { SectionLabel("Source switching") }
        item(key = "mode", contentType = "toggleGroup") {
            ConnectedToggleGroup(
                options = PlaybackMode.entries,
                selected = aggregation.playbackMode,
                onSelect = { actions.setPlaybackMode(it) },
                label = {
                    when (it) {
                        PlaybackMode.Auto -> "Auto"
                        PlaybackMode.Priority -> "Preferred source"
                        PlaybackMode.Pinned -> "Pinned source"
                    }
                },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            )
        }
        item(key = "modeNote", contentType = "note") {
            SettingsNote(
                when (aggregation.playbackMode) {
                    PlaybackMode.Auto -> "Full sources come first, then the platform order and bitrate below. When the current source fails the app switches to the next one, and the source chip on the player page explains what is playing."
                    PlaybackMode.Priority -> "Tries platforms in order and falls back down the list when one is unavailable."
                    PlaybackMode.Pinned -> "Prefers the selected platform; if it returns no URL the app still falls back and shows the actual source."
                },
            )
        }
        if (aggregation.playbackMode == PlaybackMode.Pinned) {
            item(key = "pinHead", contentType = "sectionHeader") { SectionLabel("Pinned source") }
            item(key = "pin", contentType = "chips") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pinnable.forEach { provider ->
                        MusicChip(provider.name, aggregation.pinnedProvider == provider.id) { actions.setPinnedProvider(provider.id) }
                    }
                }
            }
        }
        item(key = "priorityHead", contentType = "sectionHeader") {
            SectionLabel("Source order") {
                Text("Higher positions are tried first", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "priority", contentType = "settingRow") {
            ListRow(
                // 顺序全应用只有一份，就在'Sources'里排；这里只说明现在是什么顺序
                title = priority.take(3).joinToString(" → ") { providerName(state.providers, it) }
                    .ifBlank { "No platforms yet" },
                index = 0,
                count = 1,
                onClick = { onOpen(SettingsSection.Sources) },
                icon = Icons.Filled.Tune,
                summary = "Automatic and manual switching fall back in this order; change it under Sources",
            )
        }
        item(key = "matchHead", contentType = "sectionHeader") { SectionLabel("Match") }
        item(key = "strict", contentType = "switch") {
            SwitchRow(
                title = "Strict matching",
                checked = aggregation.matchStrictness == MatchStrictness.Strict,
                onCheckedChange = { actions.setStrictMatch(it) },
                index = 0,
                count = 3,
                summary = "Reduces false merges of live, instrumental and remastered versions",
                icon = Icons.Filled.Shield,
            )
        }
        item(key = "preview", contentType = "switch") {
            SwitchRow(
                title = "Allow preview clips",
                checked = aggregation.allowPreview,
                onCheckedChange = { actions.setAllowPreview(it) },
                index = 1,
                count = 3,
                summary = "Plays the preview clip a platform returns when no full source exists",
                icon = Icons.Filled.MusicNote,
            )
        }
        item(key = "clearMatch", contentType = "settingRow") {
            ListRow(
                title = "Clear match cache",
                index = 2,
                count = 3,
                onClick = null,
                icon = Icons.Filled.CleaningServices,
                summary = "The same recording is looked up again on the next source switch; splits and manual merges are unaffected",
                showChevron = false,
                trailing = {
                    TextButton(onClick = actions::clearMatchCache, modifier = Modifier.tvFocusRing(CircleShape)) { Text("Clear") }
                },
            )
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote("Both automatic and manual source switching follow these rules. The player always shows the source actually playing.")
            }
        }
    }
}

// ======================= Sources =======================

internal fun orderedEnabledSources(providers: List<ProviderInfo>, enabledIds: List<String>): List<ProviderInfo> {
    val byId = providers.associateBy { it.id }
    return enabledIds.distinct().mapNotNull(byId::get)
}

/**
 * Sources：启用哪些平台、按什么顺序。顺序全应用只有这一份——聚合搜索、Charts和Playlists的来源条、
 * 换源面板、播放换源都按它排。关掉的平台不参与这些；换源时碰上仍会排在最后试着用。
 */
@Composable
internal fun SourcesSettingsScreen(
    state: MusicUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    embedded: Boolean = false,
    onOpen: (SettingsSection) -> Unit,
) {
    val tv = LocalAppWindow.current.isTv
    val searchable = state.providers.filter { it.search }
    val lxRecords by LxSources.records.collectAsState()
    val orderable = searchable + ProviderInfo(
        id = LxPlatform.PROVIDER_ID,
        name = LxPlatform.PROVIDER_NAME,
        note = if (lxRecords.isEmpty()) "No scripts added yet" else "${lxRecords.count { it.enabled }} scripts enabled · order is set under Custom sources",
    )
    val enabledIds = state.aggregation.enabledOrDefault(orderable.map { it.id })
    val enabled = orderedEnabledSources(orderable, enabledIds)
    val disabled = orderable.filterNot { it.id in enabledIds }
    SettingsPage(SettingsSection.Sources, embedded, onBack) {
        item(key = "enabledHead", contentType = "sectionHeader") {
            SectionLabel("Enabled") {
                Text(
                    "${enabled.size} platforms · higher means earlier",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            searchable.isEmpty() -> item(key = "loading", contentType = "skeleton") { SkeletonRows(6) }
            enabled.isEmpty() -> item(key = "enabledEmpty", contentType = "note") {
                SettingsNote("No platform is enabled yet: aggregated search and source switching return nothing, and the Charts and Playlists source strip only shows disabled platforms.")
            }
            else -> item(key = "enabled", contentType = "sourceList") {
                SourceOrderList(enabled, tv, actions::moveSource, actions::toggleAggregateProvider)
            }
        }
        item(key = "disabledHead", contentType = "sectionHeader") { SectionLabel("Off") }
        if (disabled.isNotEmpty()) {
            item(key = "disabled", contentType = "sourceList") {
                SegmentedGroup {
                    disabled.forEachIndexed { index, provider ->
                        key(provider.id) {
                            ListRow(
                                title = provider.name,
                                index = index,
                                count = disabled.size,
                                onClick = { actions.toggleAggregateProvider(provider.id) },
                                modifier = Modifier.semantics {
                                    role = Role.Switch
                                    stateDescription = "Off"
                                },
                                leading = { ProviderMark(provider.name, 36.dp, id = provider.id) },
                                summary = capabilities(provider),
                                showChevron = false,
                                trailing = { Switch(checked = false, onCheckedChange = null) },
                            )
                        }
                    }
                }
            }
        }
        item(key = "note", contentType = "note") {
            SettingsNote("The order also decides aggregated search, the Charts and Playlists source strip, the source panel and playback fallbacks.")
        }
        item(key = "lxSources", contentType = "settingRow") {
            Box(Modifier.padding(top = 16.dp)) {
                LxSourcesSettingsEntry { onOpen(SettingsSection.LxSources) }
            }
        }
        item(key = "reset", contentType = "settingRow") {
            ListRow(
                title = "Restore default platforms and order",
                index = 0,
                count = 1,
                onClick = actions::resetSources,
                icon = Icons.Filled.Refresh,
                summary = "Affects built-in sources only; custom scripts, the source-switch mode and match strictness are unaffected",
            )
        }
    }
}

/** 已启用平台的排序列表：电话 / 平板用上下按钮，电视用 OK 拿起、方向键移动、OK 放下。 */
@Composable
private fun SourceOrderList(
    enabled: List<ProviderInfo>,
    tv: Boolean,
    onMove: (String, Int) -> Unit,
    onToggle: (String) -> Unit,
) {
    var moving by remember { mutableStateOf<String?>(null) }
    // 移动模式没有可预览的画面，手势仍然走预测性返回：松手才取消，划到一半可以反悔
    PredictiveBackHandler(enabled = moving != null) { events ->
        try {
            events.collect { }
            moving = null
        } catch (_: CancellationException) {
        }
    }
    SegmentedGroup {
        enabled.forEachIndexed { index, provider ->
            // key 保证移动后还是同一个节点，电视上焦点跟着这一行走
            key(provider.id) {
                SourceRow(
                    rank = index + 1,
                    provider = provider,
                    index = index,
                    count = enabled.size,
                    tv = tv,
                    moving = moving == provider.id,
                    onToggleMove = { moving = if (moving == provider.id) null else provider.id },
                    onMove = { delta -> onMove(provider.id, delta) },
                    onToggle = { onToggle(provider.id) },
                )
            }
        }
    }
}

@Composable
private fun SourceRow(
    rank: Int,
    provider: ProviderInfo,
    index: Int,
    count: Int,
    tv: Boolean,
    moving: Boolean,
    onToggleMove: () -> Unit,
    onMove: (Int) -> Unit,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val name = provider.name
    val shape = if (moving) RoundedCornerShape(16.dp) else segmentShape(index, count)
    // 拿起时上下键在这一行上挪，左右键不离开它；没拿起就不拦按键
    val moveKeys = if (tv && moving) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionUp -> {
                    if (index > 0) onMove(-1)
                    true
                }
                Key.DirectionDown -> {
                    if (index < count - 1) onMove(1)
                    true
                }
                Key.DirectionLeft, Key.DirectionRight -> true
                else -> false
            }
        }
    } else {
        Modifier
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (tv) Modifier.tvFocusRing(shape, scale = 1.02f) else Modifier)
            .clip(shape)
            .background(if (moving) scheme.secondaryContainer else scheme.surfaceContainerLow)
            .then(moveKeys)
            // 整行可点就是开关（读屏角色和 Switch 都由整行给出）；拿起时按 OK 是放下
            .clickable(
                onClickLabel = if (moving) "Put down" else "Turn off $name",
                onClick = { if (moving) onToggleMove() else onToggle() },
            )
            .semantics {
                role = Role.Switch
                stateDescription = if (moving) "Moving" else "Position $rank, enabled"
            }
            .padding(start = 8.dp, end = if (tv) 12.dp else 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            // 电视上名次已由整行的“第 N 位”读出
            Modifier.width(28.dp).then(if (tv) Modifier.clearAndSetSemantics {} else Modifier),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (moving) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        )
        ProviderMark(name, 36.dp, id = provider.id)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (moving) scheme.onSecondaryContainer else scheme.onSurface,
            )
            Text(
                capabilities(provider),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Switch 只作显示：开关由整行点，和别的设置行一致
        Switch(checked = true, onCheckedChange = null)
        if (tv) {
            IconButton(onClick = onToggleMove, modifier = Modifier.tvFocusRing(CircleShape)) {
                Icon(
                    if (moving) Icons.Filled.SwapVert else Icons.Filled.DragHandle,
                    if (moving) "Put down $name" else "Pick up $name",
                    tint = if (moving) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                )
            }
        } else {
            IconButton(onClick = { onMove(-1) }, enabled = index > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, "Move $name up")
            }
            IconButton(onClick = { onMove(1) }, enabled = index < count - 1) {
                Icon(Icons.Filled.KeyboardArrowDown, "Move $name down")
            }
        }
    }
}

// ======================= 搜索与浏览 =======================

/** 搜索与浏览：Default browse source（点开选择）+ 跳到Sources管理平台的启停与顺序。 */
@Composable
internal fun SearchBrowseSettingsScreen(
    state: MusicUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    embedded: Boolean = false,
    onOpen: (SettingsSection) -> Unit = {},
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val currentName = providerName(state.providers, state.providerId)
    val searchable = state.providers.filter { it.search }
    val enabled = state.aggregation.enabledOrDefault(searchable.map { it.id }).size
    SettingsPage(SettingsSection.Search, embedded, onBack) {
        item(key = "browseHead", contentType = "sectionHeader") { SectionLabel("Default browse source") }
        item(key = "browse", contentType = "settingRow") {
            ListRow(
                title = currentName,
                index = 0,
                count = 2,
                onClick = { pickerOpen = true },
                leading = { ProviderMark(currentName, 40.dp, active = true, id = state.providerId) },
                summary = "Source for Charts and Playlists; does not affect playback source switching",
            )
        }
        item(key = "sources", contentType = "settingRow") {
            ListRow(
                title = "Sources",
                index = 1,
                count = 2,
                onClick = { onOpen(SettingsSection.Sources) },
                icon = Icons.Filled.Tune,
                summary = "$enabled platforms enabled · toggles and order",
            )
        }
        item(key = "note", contentType = "note") {
            SettingsNote("More platforms mean fuller results and possibly slower search and matching. Turn off unused platforms and reorder them under Sources.")
        }
    }
    if (pickerOpen) {
        BrowseSourceSheet(state, actions, onDismiss = { pickerOpen = false })
    }
}

private fun capabilities(provider: ProviderInfo): String {
    val caps = listOfNotNull(
        "Track".takeIf { provider.search },
        "Playlists".takeIf { provider.searchPlaylist },
        "Album".takeIf { provider.searchAlbum },
        "Artists".takeIf { provider.searchArtist },
        "Charts".takeIf { provider.topLists },
        "Lyrics".takeIf { provider.lyrics },
    ).joinToString(" · ")
    return listOf(caps, provider.note).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun BrowseSourceSheet(state: MusicUiState, actions: MusicActions, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    SettingsTvFocus(focus)
    AdaptiveSheet(onDismiss = onDismiss, title = "Default browse source", subtitle = "Shows this platform first when Charts or Playlists opens") {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
        ) {
            state.providers.forEachIndexed { index, provider ->
                val selected = provider.id == state.providerId
                ListRow(
                    title = provider.name,
                    index = index,
                    count = state.providers.size,
                    onClick = {
                        actions.selectProvider(provider.id)
                        onDismiss()
                    },
                    // 单选列表：每行都读出选中 / 未选中
                    modifier = (if (selected) Modifier.focusRequester(focus) else Modifier).semantics {
                        role = Role.RadioButton
                        this.selected = selected
                    },
                    leading = { ProviderMark(provider.name, 36.dp, active = selected, id = provider.id) },
                    selected = selected,
                    showChevron = false,
                    trailing = if (selected) {
                        { Icon(Icons.Filled.Check, null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

// ======================= 平台账号 =======================

/**
 * 平台账号：每个平台一行，已登录用 primary 标出，Not signed in给出登录按钮。
 * 电视上只提供Sign in with QR code，不让用户在电视上输入 Cookie。
 */
@Composable
fun AccountsSettingsScreen(state: MusicUiState, actions: MusicActions, onBack: () -> Unit, embedded: Boolean = false) {
    val tv = LocalAppWindow.current.isTv
    LaunchedEffect(Unit) { actions.syncIfStaleOnScreen() }
    // 头像、昵称、会员等级只在冷启动拉过一次；进这个页面就重拉，失败也有上次缓存兜着
    LaunchedEffect(Unit) { actions.refreshAccountProfiles() }
    var cookieFor by remember { mutableStateOf<ProviderInfo?>(null) }
    var qrFor by remember { mutableStateOf<ProviderInfo?>(null) }
    var passwordFor by remember { mutableStateOf<ProviderInfo?>(null) }
    var signerOpen by remember { mutableStateOf(false) }
    val providers = state.providers
    val loggedCount = providers.count { !state.cookies[it.id].isNullOrBlank() }
    SettingsPage(
        SettingsSection.Accounts,
        embedded,
        onBack,
        subtitle = if (loggedCount > 0) "Signed in to $loggedCount platforms" else stringResource(SettingsSection.Accounts.subtitleRes),
    ) {
        item(key = "intro", contentType = "note") {
            SettingsNote(
                if (tv) "Platform accounts are independent. On TV, sign in by scanning with your phone; cookies stay on this device and, once sync is connected, are written in plain text into the private sync file."
                else "Platform accounts are independent. Cookies are used for source switching and VIP checks; once sync is connected they are written in plain text into the private sync file, so never share the file link. Tap a platform to paste or update its cookie.",
                Modifier.padding(bottom = 8.dp),
            )
        }
        if (providers.isEmpty()) {
            item(key = "loading", contentType = "skeleton") { SkeletonRows(5) }
        } else {
            itemsIndexed(
                providers,
                key = { _, provider -> "account:${provider.id}" },
                contentType = { _, _ -> "provider" },
            ) { index, provider ->
                AccountRow(
                    provider = provider,
                    loggedIn = !state.cookies[provider.id].isNullOrBlank(),
                    profile = state.accountProfiles[provider.id],
                    index = index,
                    count = providers.size,
                    tv = tv,
                    onQr = { qrFor = provider },
                    onCookie = { cookieFor = provider },
                    onPassword = { passwordFor = provider },
                    onLogout = { actions.logout(provider.id) },
                    onImport = { actions.openPlatformImport(provider.id) },
                    onResetPrompt = { actions.resetPlatformImportPrompt(provider.id) },
                )
            }
        }
        if (providers.any { it.id == "soda" }) {
            item(key = "soda_signer", contentType = "signerGroup") {
                Column(Modifier.padding(top = 12.dp)) {
                    SectionLabel("Soda playback")
                    ListRow(
                        title = "Soda playback signer",
                        index = 0,
                        count = 1,
                        onClick = { signerOpen = true },
                        // 图标用 SodaM 官方 logo：默认连的就是它的公共Signing service
                        leading = {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) { SodamLogo(40.dp, Modifier.clip(CircleShape)) }
                        },
                        summary = signerSummary(state),
                    )
                }
            }
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote(
                    if (tv) "Sign in with a QR code on TV; for platforms that support passwords, enter them with the remote."
                    else "Whether a QR code works depends on the platform API; for platforms with password sign-in, tap Sign in and enter your username and password.",
                )
            }
        }
    }
    if (signerOpen) {
        SodaSignerDialog(
            initialUrl = state.sodaSignerUrl,
            initialToken = state.sodaSignerToken,
            initialEnabled = state.sodaSignerEnabled,
            onSave = { url, token, enabled ->
                actions.saveSodaSigner(url, token, enabled)
                signerOpen = false
            },
            onDismiss = { signerOpen = false },
        )
    }
    qrFor?.let { provider ->
        QrLoginPanel(
            provider = provider,
            state = state,
            actions = actions,
            onCookie = if (tv) null else ({
                actions.cancelLogin()
                qrFor = null
                cookieFor = provider
            }),
            onDismiss = {
                actions.cancelLogin()
                qrFor = null
            },
        )
    }
    cookieFor?.let { provider ->
        CookieDialog(
            provider = provider,
            initial = state.cookies[provider.id].orEmpty(),
            onSave = { value ->
                actions.saveCookie(provider.id, value)
                cookieFor = null
            },
            onDismiss = { cookieFor = null },
        )
    }
    passwordFor?.let { provider ->
        PasswordLoginDialog(
            provider = provider,
            state = state,
            onDismiss = {
                actions.cancelLogin()
                passwordFor = null
            },
            onSubmit = { server, username, password ->
                actions.loginWithPassword(provider.id, username, password, server)
            },
        )
    }
}

@Composable
private fun AccountRow(
    provider: ProviderInfo,
    loggedIn: Boolean,
    profile: AccountProfile?,
    index: Int,
    count: Int,
    tv: Boolean,
    onQr: () -> Unit,
    onCookie: () -> Unit,
    onPassword: () -> Unit,
    onLogout: () -> Unit,
    onImport: () -> Unit,
    onResetPrompt: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // 已登录且拿到资料时展示账号卡片：头像、昵称、会员/等级；拿不到资料就保持原来的平台行
    val avatar = profile?.avatar.orEmpty()
    val summary = when {
        loggedIn -> {
            val info = profile?.let { p -> listOf(p.nickname, p.detail).filter { it.isNotBlank() }.joinToString(" · ") }.orEmpty()
            info.ifBlank { "Signed in" }
        }
        !provider.loginRequired -> "No sign-in needed"
        provider.passwordLogin -> "Not signed in · username and password"
        provider.qrLogin -> "Not signed in"
        tv -> "Not signed in · sign in on your phone"
        else -> "Not signed in · cookie only"
    }
    ListRow(
        title = provider.name,
        index = index,
        count = count,
        onClick = when {
            provider.passwordLogin -> onPassword
            tv || !provider.loginRequired -> null
            else -> onCookie
        },
        leading = {
            if (loggedIn && avatar.isNotBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
            } else {
                ProviderMark(provider.name, 40.dp, active = loggedIn, id = provider.id)
            }
        },
        summary = summary,
        showChevron = false,
        trailing = {
            when {
                loggedIn -> {
                    TextButton(onClick = onLogout, modifier = Modifier.tvFocusRing(CircleShape)) {
                        Text("Sign out", Modifier.semantics { contentDescription = "Sign out of ${provider.name}" }, color = MaterialTheme.colorScheme.error)
                    }
                    // 登录后引导导入的平台Playlists：随时能再打开
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.tvFocusRing(CircleShape),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                "More actions: ${provider.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Import my playlists…") },
                                onClick = {
                                    menuOpen = false
                                    onImport()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Ask again on next sign-in") },
                                onClick = {
                                    menuOpen = false
                                    onResetPrompt()
                                },
                            )
                        }
                    }
                }
                provider.passwordLogin -> FilledTonalButton(onClick = onPassword, modifier = Modifier.tvFocusRing(CircleShape)) {
                    Text("Sign in", Modifier.semantics { contentDescription = "Sign in to ${provider.name}" })
                }
                provider.qrLogin -> FilledTonalButton(onClick = onQr, modifier = Modifier.tvFocusRing(CircleShape)) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign in", Modifier.semantics { contentDescription = "Sign in to ${provider.name} with a QR code" })
                }
                provider.loginRequired && !tv -> FilledTonalButton(onClick = onCookie) { Text("Cookie", Modifier.semantics { contentDescription = "Sign in to ${provider.name} with a cookie" }) }
            }
        },
    )
}

/**
 * One scan channel: [type] is passed to the platform layer ("qq" for QQ, "wx" for WeChat); the rest goes to the UI.
 */
private data class QrChannel(val type: String, val label: String, val subtitle: String)

/**
 * QQ 的会员可能挂在 QQ 号、也可能挂在微信号上（同一个 QQ 音乐账号两种登录方式都认），
 * 所以这里给两条扫码通道；其他平台只有默认一条。
 *
 * 微信放第一位：QQ 那条走 ptlogin2 三方登录，现在上游拿不到有效的设备风控参数，
 * 二维码扫了也确认不了（ptqrlogin 直接 403），默认给一条能用的。
 */
private fun qrLoginChannels(provider: ProviderInfo): List<QrChannel> = when (provider.id) {
    "qq" -> listOf(
        QrChannel("wx", "WeChat QR code", "Scan the QR code with WeChat and confirm the sign-in on your phone"),
        QrChannel("", "QQ QR code", "Scan the QR code with QQ and confirm the sign-in on your phone"),
    )
    else -> listOf(QrChannel("", "Sign in with QR code", "Scan the QR code with the ${provider.name} app and confirm the sign-in on your phone"))
}

/** Sign in with QR code：手机是底部面板，平板和电视是居中对话框（电视二维码放大，方便在沙发上扫）。 */
@Composable
private fun QrLoginPanel(
    provider: ProviderInfo,
    state: MusicUiState,
    actions: MusicActions,
    onCookie: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val window = LocalAppWindow.current
    val channels = remember(provider.id) { qrLoginChannels(provider) }
    var type by remember(provider.id) { mutableStateOf(channels.first().type) }
    SideEffect(provider.id, type, actions) { actions.startQrLogin(provider.id, type) }
    // 面板不经 onDismiss 离开组合时（旋转屏幕、窗口尺寸变化，qrFor 不会保存）也要停掉轮询
    DisposableEffect(provider.id) { onDispose { actions.cancelLogin() } }
    // 登录成功后 Cookie 会变，变了就关闭
    val initialCookie = remember(provider.id) { state.cookies[provider.id].orEmpty() }
    val cookie = state.cookies[provider.id].orEmpty()
    SideEffect(provider.id, cookie, initialCookie) {
        if (cookie.isNotBlank() && cookie != initialCookie) onDismiss()
    }
    val title = "Sign in to ${provider.name} with a QR code"
    val subtitle = channels.firstOrNull { it.type == type }?.subtitle
        ?: "Scan the QR code with the ${provider.name} app and confirm the sign-in on your phone"
    val refresh = { actions.startQrLogin(provider.id, type) }
    if (window.isPhone) {
        AdaptiveSheet(onDismiss = onDismiss, title = title, subtitle = subtitle) {
            QrLoginBody(provider, state, channels, type, { type = it }, refresh, onCookie)
        }
        return
    }
    Dialog(onDismissRequest = onDismiss) {
        DialogAvoidsFold()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = if (window.isTv) 520.dp else 440.dp),
        ) {
            Column(Modifier.padding(vertical = 24.dp)) {
                Text(
                    title,
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    subtitle,
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QrLoginBody(provider, state, channels, type, { type = it }, refresh, onCookie)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QrLoginBody(
    provider: ProviderInfo,
    state: MusicUiState,
    channels: List<QrChannel>,
    type: String,
    onTypeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCookie: (() -> Unit)?,
) {
    val window = LocalAppWindow.current
    val active = state.loginProviderId == provider.id
    val login = if (active) state.login else null
    val status = if (active) state.loginStatus else ""
    val ended = status == "QR code expired" || status.contains("Failed")
    val qrSize = when {
        window.isTv -> 300.dp
        window.isPhone -> 220.dp
        else -> 240.dp
    }
    val refreshFocus = remember { FocusRequester() }
    SettingsTvFocus(refreshFocus, ended)
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 通道不止一条时才给选择；切通道会重新取码，微信那条要用微信扫
        if (channels.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                channels.forEach { channel ->
                    MusicChip(
                        channel.label,
                        type == channel.type,
                        description = "Sign in to ${provider.name} with a QR code: ${channel.label}",
                    ) { onTypeChange(channel.type) }
                }
            }
        }
        Box(
            Modifier
                .size(qrSize)
                .clip(RoundedCornerShape(24.dp))
                // 二维码需要浅底深码，扫码器才认，不跟随主题
                .background(Color.White)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            val image = login?.session?.imageUrl.orEmpty()
            val url = login?.session?.url.orEmpty()
            when {
                image.startsWith("data:") -> {
                    // 平台直接返回的二维码图片：解码放后台线程，放大时用最近邻，保持边缘清晰
                    val bitmap by produceState<ImageBitmap?>(initialValue = null, image) {
                        value = withContext(Dispatchers.Default) {
                            runCatching {
                                val bytes = android.util.Base64.decode(image.substringAfter(','), android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    val decoded = bitmap
                    if (decoded != null) {
                        androidx.compose.foundation.Image(
                            decoded,
                            "Sign-in QR code",
                            Modifier.fillMaxSize(),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        )
                    }
                }
                image.isNotBlank() -> AsyncImage(model = image, contentDescription = "Sign-in QR code", modifier = Modifier.fillMaxSize())
                url.isNotBlank() -> QrCodeImage(url, Modifier.fillMaxSize())
                login == null && !ended -> LoadingIndicator(Modifier.size(56.dp))
                else -> Icon(Icons.Filled.ErrorOutline, "No QR code", Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            }
            if (ended && login != null) {
                // 过期或失败后盖住旧二维码，免得被误扫
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (status == "QR code expired") "Expired" else "Expired",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Row(
            Modifier.padding(top = 12.dp).heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!ended) LoadingIndicator(Modifier.size(28.dp))
            Text(
                status.ifBlank { "Getting the QR code…" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (ended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (onCookie != null) {
                TextButton(onClick = onCookie) { Text("Use a cookie instead") }
            }
            if (ended) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.focusRequester(refreshFocus).heightIn(min = 48.dp).tvFocusRing(CircleShape),
                ) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh QR code")
                }
            } else {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.focusRequester(refreshFocus).heightIn(min = 48.dp).tvFocusRing(CircleShape),
                ) { Text("Refresh QR code") }
            }
        }
        Text(
            "After signing in you are asked once whether to import this account's playlists as platform-linked playlists; you can turn it off.",
            Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CookieDialog(provider: ProviderInfo, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember(provider.id) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogAvoidsFold()
            Text("${provider.name} Cookie")
        },
        text = {
            Column {
                Text(
                    cookieHint(provider),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 100.dp),
                    placeholder = { Text("Paste cookie") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Username + 密码登录；成功后 Cookie 变化，面板自动关闭。
 * 自建服务器（[ProviderInfo.serverLogin]）多一个服务器地址输入框，密码只存本机。
 */
@Composable
private fun PasswordLoginDialog(
    provider: ProviderInfo,
    state: MusicUiState,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
) {
    var server by remember(provider.id) { mutableStateOf("") }
    var username by remember(provider.id) { mutableStateOf("") }
    var password by remember(provider.id) { mutableStateOf("") }
    val cookie = state.cookies[provider.id].orEmpty()
    val initialCookie = remember(provider.id) { cookie }
    SideEffect(provider.id, cookie, initialCookie) {
        if (cookie.isNotBlank() && cookie != initialCookie) onDismiss()
    }
    val active = state.loginProviderId == provider.id
    val status = if (active) state.loginStatus else ""
    val busy = status == "Signing in…"
    val error = status.takeIf { it.startsWith("Sign-in failed") }.orEmpty()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            DialogAvoidsFold()
            Text("Sign in to ${provider.name}")
        },
        text = {
            Column {
                Text(
                    if (provider.serverLogin) {
                        "Sign in with your own Navidrome / Subsonic server. The password stays on this device " +
                            "and is never synced; only the address and username are kept as a marker."
                    } else {
                        "Uses a music.op.wiki account; register on the site first if you have none. After signing in you can sync, play and edit your playlists; creating playlists is still done on the site."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (provider.serverLogin) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("Server address") },
                        placeholder = { Text("https://music.example.com") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                        ),
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth().padding(top = if (provider.serverLogin) 8.dp else 12.dp),
                    label = { Text("Username") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    ),
                )
                if (error.isNotBlank()) {
                    Text(
                        error.removePrefix("Sign-in failed: "),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(server, username, password) },
                enabled = username.isNotBlank() && password.isNotBlank() &&
                    (!provider.serverLogin || server.isNotBlank()) && !busy,
            ) { Text(if (busy) "Signing in…" else "Sign in") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}

/** 个别平台的 Cookie 有固定格式，按平台给提示；没有特殊要求的用通用说明。 */
private fun cookieHint(provider: ProviderInfo): String = when (provider.id) {
    // Apple 的Lyrics在 /songs/{id}/lyrics 子资源上，只有带 media-user-token 才返回；
    // 商店还要和歌曲所属目录一致，中国区账号需要显式声明 storefront。
    "apple" -> "Paste the Apple Music web player cookie; it must contain media-user-token, otherwise lyrics can't be fetched." +
        "Separate fields with semicolons; add storefront=cn for a China-region account, otherwise the US catalog is searched and lyrics are fetched for it."
    // 私人Playlists和账号资料靠 SAPISIDHASH 鉴权，缺 SAPISID 就只剩公开内容。
    "youtubemusic" -> "Paste the full music.youtube.com cookie; it must contain SAPISID or __Secure-3PAPISID." +
        "You can search, play and browse public playlists without signing in; your private playlists appear only after sign-in."
    "mhub" -> "Sign in with your username and password; you can also paste a session cookie such as mhub_session=..."
    else -> "Stored only on this device and used for source switching and VIP checks. Saving it empty signs you out."
}

/** 签名器行的副标题：默认连 SodaM public服务，自填地址或关掉都要说清楚。 */
private fun signerSummary(state: MusicUiState): String = when {
    !state.sodaSignerEnabled -> "Off · member full tracks return preview clips only"
    state.sodaSignerUrl.isBlank() -> "SodaM public signing service · member full tracks available"
    else -> "Custom · ${signerLabel(state.sodaSignerUrl)}"
}

private fun signerLabel(url: String): String {
    val parsed = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return url
    val host = parsed.host ?: return url
    return if (parsed.port > 0) "$host:${parsed.port}" else host
}

/**
 * 汽水签名器：协议与 sodam / libresoda / libmssdk 的 POST /sign 一致，
 * 签名器需要看到即将发出的 URL、body 和全部请求头。
 * 默认用 SodaM 的公共Signing service，也可以换成自建实例。
 */
@Composable
private fun SodaSignerDialog(
    initialUrl: String,
    initialToken: String,
    initialEnabled: Boolean,
    onSave: (String, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var custom by remember { mutableStateOf(initialUrl.isNotBlank()) }
    var url by remember { mutableStateOf(initialUrl) }
    var token by remember { mutableStateOf(initialToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogAvoidsFold()
            Text("Soda playback signer")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SodamCredit(note = "Per-request signing for member full tracks comes from SodaM; tap to open the project")
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable signer", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (enabled) "Member accounts can play full tracks and lossless audio" else "When off, member tracks only get public preview clips",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if (enabled) {
                    Text(
                        "Signing service",
                        Modifier.padding(top = 14.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    ConnectedToggleGroup(
                        options = listOf(false, true),
                        selected = custom,
                        onSelect = { custom = it },
                        label = { if (it) "Custom service" else "SodaM public" },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (custom) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            label = { Text("Signing service URL") },
                            placeholder = { Text("http://192.168.1.10:8899/sign") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text("Token (optional)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                    } else {
                        Text(
                            "A public instance run by SodaM's author, and the default for the desktop build. Signing requests send your cookie " +
                                "and full request headers are sent; use it only on networks you trust.",
                            Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                // 选公共服务时清掉自填地址，回落到内置的 SodaM publicSigning service
                onClick = {
                    onSave(if (custom) url else "", if (custom) token else "", enabled)
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ======================= 存储 =======================

/** 存储：总占用大字 + 分段占用条 + 分项清除。清除缓存不影响Playlists、收藏和下载，不需要二次确认。 */
@Composable
fun StorageSettingsScreen(
    cacheBytes: Long,
    downloadsBytes: Long,
    downloadsCount: Int,
    onClearCache: () -> Unit,
    onClearMatch: () -> Unit,
    cacheLimitMb: Int,
    onCacheLimit: (Int) -> Unit,
    onOpenDownloads: () -> Unit,
    onBack: () -> Unit,
    embedded: Boolean = false,
) {
    SettingsPage(SettingsSection.Storage, embedded, onBack, subtitle = "App uses ${formatBytes(cacheBytes + downloadsBytes)}") {
        item(key = "usage", contentType = "storageUsage") { StorageUsageCard(cacheBytes, downloadsBytes) }
        item(key = "limitHead", contentType = "sectionHeader") { SectionLabel("Playback cache limit") }
        item(key = "limit", contentType = "cacheLimit") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectedToggleGroup(
                    options = listOf(128, 256, 512, 1024),
                    selected = cacheLimitMb,
                    onSelect = onCacheLimit,
                    label = { if (it >= 1024) "${it / 1024} GB" else "$it MB" },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
                SettingsNote(
                    if (LocalAppWindow.current.isTv) "TV boxes have little storage, so the default is 256 MB. Over the limit, the least recently played cache is deleted first."
                    else "Default 512 MB. Over the limit, the least recently played cache is deleted first; lowering it applies immediately.",
                )
            }
        }
        item(key = "rows", contentType = "settingsGroup") {
            SegmentedGroup(Modifier.padding(top = 12.dp)) {
                ListRow(
                    title = "Playback cache",
                    index = 0,
                    count = 3,
                    onClick = null,
                    icon = Icons.Filled.History,
                    iconTone = IconTone.Tertiary,
                    summary = "${formatBytes(cacheBytes)} · playback re-caches after clearing",
                    showChevron = false,
                    trailing = {
                        TextButton(onClick = onClearCache, enabled = cacheBytes > 0, modifier = Modifier.tvFocusRing(CircleShape)) { Text("Clear") }
                    },
                )
                ListRow(
                    title = "Downloads",
                    index = 1,
                    count = 3,
                    onClick = onOpenDownloads,
                    icon = Icons.Filled.Download,
                    iconTone = IconTone.Primary,
                    summary = "$downloadsCount tracks · ${formatBytes(downloadsBytes)}",
                    value = "Manage",
                )
                ListRow(
                    title = "Match cache",
                    index = 2,
                    count = 3,
                    onClick = null,
                    icon = Icons.Filled.CleaningServices,
                    summary = "The next source switch matches again; splits and manual merges are unaffected",
                    showChevron = false,
                    trailing = {
                        TextButton(onClick = onClearMatch, modifier = Modifier.tvFocusRing(CircleShape)) { Text("Clear") }
                    },
                )
            }
        }
        item(key = "note", contentType = "note") { SettingsNote("Clearing the cache does not affect playlists, favorites or downloads. Downloads are the only offline source; manage them under Downloads.") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageUsageCard(cacheBytes: Long, downloadsBytes: Long) {
    val scheme = MaterialTheme.colorScheme
    val total = cacheBytes + downloadsBytes
    val parts = listOf(
        Triple("Download", downloadsBytes, scheme.primary),
        Triple("Playback cache", cacheBytes, scheme.tertiary),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(20.dp),
    ) {
        Text("App uses", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text(formatBytes(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Row(
            Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val visible = parts.filter { it.second > 0 }
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize().background(scheme.surfaceVariant))
            } else {
                visible.forEach { (_, bytes, color) ->
                    // 很小的一项也留出能看见的宽度
                    Box(
                        Modifier
                            .weight(maxOf(bytes.toFloat(), total * 0.02f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            parts.forEach { (label, bytes, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Text("$label ${formatBytes(bytes)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ======================= Play reporting =======================

/**
 * Play reporting：把'听了什么、听了多久、听完了没有'回传给平台。
 * 汽水的推荐流、每日推荐、听歌模式都靠这个才有个人口味，关掉之后首页推荐会一直是通用内容。
 */
@Composable
fun ReportSettingsScreen(state: MusicUiState, actions: MusicActions, onBack: () -> Unit, embedded: Boolean = false) {
    val providers = remember { PlayReportHub.reportingProviders() }
    SettingsPage(SettingsSection.Report, embedded, onBack) {
        item(key = "switch", contentType = "switch") {
            SwitchRow(
                title = "Play reporting",
                checked = state.playReportEnabled,
                onCheckedChange = actions::setPlayReportEnabled,
                index = 0,
                count = 1,
                summary = if (state.playReportEnabled) {
                    "Listening history is reported back to each platform so recommendations follow your taste"
                } else {
                    "Off: no play reports are sent; recommendations use popular content only"
                },
                icon = Icons.Filled.Analytics,
            )
        }
        item(key = "platformsHead", contentType = "sectionHeader") { SectionLabel("Platforms that report") }
        item(key = "platforms", contentType = "platformRows") {
            Column {
                providers.forEachIndexed { index, id ->
                    ListRow(
                        title = PlayReportHub.displayName(id),
                        index = index,
                        count = providers.size,
                        onClick = null,
                        icon = Icons.Filled.CheckCircle,
                        summary = platformReportSummary(id, state),
                        showChevron = false,
                    )
                }
            }
        }
        item(key = "what", contentType = "note") {
            SettingsNote(
                "Reports include the track ID, actual listened time (pauses excluded), whether it finished, and the reason for a track change, pause or queue change." +
                    "Soda also includes recently played tracks and durations in its recommendation requests, and adjusts recommendations accordingly." +
                    "No usernames, cookies, playback URLs or file contents are reported.",
            )
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 4.dp)) {
                InfoNote("The switch only affects play reporting; search, streaming, lyrics and downloads are unaffected. Records already sent are not withdrawn when you turn it off.")
            }
        }
    }
}

/** 平台上报的当前状态：各平台自己的说明 + 有没有登录态。 */
private fun platformReportSummary(providerId: String, state: MusicUiState): String {
    val reporter = PlayReportHub.reporter(providerId) ?: return ""
    val detail = reporter.reportSummary().ifBlank { "Reporting" }
    val login = when {
        !state.playReportEnabled -> "Master switch is off"
        state.cookies[providerId].isNullOrBlank() -> "Not signed in: device-only stats"
        else -> "Signed in"
    }
    return "$detail\n$login"
}

// ======================= 开源许可 / 隐私说明 =======================

private const val LICENSE_URL = "https://github.com/mytv-android/myMusic/blob/main/LICENSE"
private const val NOTICES_URL = "https://github.com/mytv-android/myMusic/blob/main/THIRD_PARTY_NOTICES.md"
private const val GPL3_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** 移植的 MIT Lyrics动画模组：MIT 要求随副本附上版权行和许可原文，这里原样给出。 */
private const val FOLIA_MIT_NOTICE =
    "Copyright (c) 2026 baiyangmuzz（miside-drop）\n" +
        "Copyright (c) 2026 pomepii（wave-light、vinyl-disc）\n" +
        "Copyright (c) 2026 w0w1w2（stardrift）\n" +
        "Copyright (c) 2026 LingShiQwQ（bpm-style-director）\n\n" +
        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated " +
        "documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation " +
        "the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to " +
        "permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
        "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO " +
        "THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, " +
        "TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."

@Composable
fun LicensesScreen(onBack: () -> Unit, embedded: Boolean = false) {
    val openUri = rememberOpenUri()
    val thirdParty = listOf(
        "music-lib" to "Source of platform capabilities. Pinned at commit 90b2942; the Go sources are kept for porting reference and license notes.",
        "music-lib / Kotlin port" to "Built with OkHttp, platform crypto and Kotlin; no GoMobile, AAR or extra backend.",
        "music-lib / Spotify" to "Anonymous Web Player search, public embed and chart mirror fallbacks, and the Web API client-credentials flow for Spotify are ported from music-lib's spotify package (AGPL-3.0).",
        "music-lib / quality ladder" to "Per-platform quality tiers, bitrate labels and the no-silent-downgrade rule are ported from music-lib's model/quality.go and each platform's quality.go (AGPL-3.0).",
        "SodaM / libresoda" to "Source of the Soda signing protocol (POST /sign, GET /config, x-helios / x-medusa), the streaming chain and encrypted-stream decryption (AGPL-3.0). The app connects to SodaM's public signing service by default; switch to your own instance in settings.",
        "MHub / music.op.wiki" to "YouTube Music audio preparation uses the public MHub instance (AGPL-3.0). The app only calls its HTTP API and does not bundle its code; audio is prepared track by track on the server and the client requests serially within its rate limits.",
        "AndroidX、Media3、Coil、Room、WorkManager" to "All are used under Apache-2.0 or their respective open-source licenses; the full list ships with the app and repository.",
        "QR code generation" to "Sign-in QR codes are generated on this device with the Nayuki QR Code generator algorithm (MIT).",
        "LX Music / QuickJS" to "Custom sources use the LX Music mobile custom-source protocol adapter (Apache-2.0) and QuickJS Wrapper 2.4.0 (Apache-2.0). The pdone source collection is only an import entry point; its scripts are not bundled.",
        "MaterialKolor、Haze" to "Dynamic color (MIT) and blur effects (Apache-2.0).",
        "Lyricon / Lyric Stage" to "Uses the official Provider / Subscriber SDK 0.1.70 to exchange songs, lyrics and playback state with the on-device Lyricon service (Apache-2.0).",
        "YesPlayMusic" to "The Rhythm lyric backdrop's per-band adaptive gain and spectral-flux beat detection follow its MIT implementation; audio analysis now runs on local ExoPlayer PCM.",
        "Roboto Flex font" to "Latin letters and digits use Roboto Flex (SIL Open Font License 1.1), bundled as a subset with only the weight axis and Latin characters.",
        "folia-major（Folia）" to "The full-screen lyric animation interface follows the Folium v1.x docs; Aurora and 52Hz are ported from its official sample modules; the queue and playlist poster wall follows its Lattice layout. All AGPL-3.0.",
        "Folia community lyric animation modules" to "Cinerama and Jump are ported from tumuyan's cinerama 1.0.0 and visualizer-jump 1.2.0 (AGPL-3.0);" +
            "MiSide drop, wave light, vinyl disc and stardrift are ported from miside-drop, wave-light, vinyl-disc and stardrift (MIT)," +
            "The by-tempo animation switching follows bpm-style-director (MIT). Everything is rewritten natively in Kotlin; the original module's code and assets are neither run nor bundled.",
        "Rush（shub39）" to "The Rush lyric animation is rewritten in Kotlin after Rush 7.2.1's lyric page: a color sampled from the cover, large left-aligned text, depth blur, per-character feather wipe, three interlude dots and six backdrops." +
            "GPL-3.0-or-later, combined with this app's AGPL-3.0 code under section 13 of both licenses. Its fonts and icons are not bundled.",
    )
    SettingsPage(SettingsSection.Licenses, embedded, onBack) {
        item(key = "app", contentType = "licenseCard") {
            TextCard(
                title = "GNU Affero General Public License v3.0",
                body = "myMusic's app code and ported platform layer are released under AGPL-3.0.",
                index = 0,
                count = 1,
                footer = {
                    TextButton(onClick = { openUri(LICENSE_URL) }, modifier = Modifier.tvFocusRing(CircleShape)) {
                        Text("View full text")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    }
                },
            )
        }
        item(key = "thirdHead", contentType = "sectionHeader") { SectionLabel("Upstream and third-party") }
        itemsIndexed(
            thirdParty,
            key = { _, item -> "license:${item.first}" },
            contentType = { _, _ -> "license" },
        ) { index, (title, body) ->
            TextCard(title, body, index, thirdParty.size)
        }
        item(key = "gplHead", contentType = "sectionHeader") { SectionLabel("GPL-3.0 license") }
        item(key = "gpl", contentType = "licenseCard") {
            TextCard(
                title = "GNU General Public License v3.0 (Rush lyric style)",
                body = "Copyright (C) 2026 Shubham Gorai. Rewritten files keep the original header and copyright lines, note the changes and date, and are distributed under GPL v3 or later.",
                index = 0,
                count = 1,
                footer = {
                    TextButton(onClick = { openUri(GPL3_URL) }, modifier = Modifier.tvFocusRing(CircleShape)) {
                        Text("View full text")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    }
                },
            )
        }
        item(key = "mitHead", contentType = "sectionHeader") { SectionLabel("MIT license text") }
        item(key = "mit", contentType = "licenseCard") {
            TextCard(
                title = "MIT License (MiSide drop, wave light, vinyl disc, stardrift, by tempo)",
                body = FOLIA_MIT_NOTICE,
                index = 0,
                count = 1,
                footer = {
                    TextButton(onClick = { openUri(NOTICES_URL) }, modifier = Modifier.tvFocusRing(CircleShape)) {
                        Text("View full third-party notices")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    }
                },
            )
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote("If you redistribute, keep the license, copyright notices and change notes, and comply with AGPL-3.0's source-availability requirement.")
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit, embedded: Boolean = false) {
    val blocks = listOf(
        "Stay on this device" to "Recently played, downloads, playback progress and the match cache are stored on this device only. Sync tokens and WebDAV passwords are encrypted with the Android Keystore and kept locally.",
        "Local music" to "Scanning only reads audio files, never images or videos, and never modifies them; cover caches and scan records stay on this device, and so do match results unless the track is added to a synced playlist or your favorites (see below). For automatic matching, tagged tracks send their title and artist to search on enabled platforms; untagged clips are cut to 3 seconds, downsampled to 8 kHz mono and turned into a fingerprint, and only the fingerprint goes to NetEase Cloud Music's recognition service - the audio itself never leaves the device. For local tracks added to synced playlists or favorites, the sync file holds only: title, artist, album, duration, file name, file fingerprint, the name of the device holding the file, the cloud copy location and the match result (which platform's track it matched). It holds no file paths, audio, lyrics or cover images, and no playback URLs or WebDAV passwords. Audio files are uploaded only when you tap upload: to your own NetEase cloud drive or your own WebDAV server, sending the original file, by default over Wi-Fi only. Adding a device-only track to a synced playlist shows a reminder and never uploads automatically. The QQ Music cloud drive is read-only here: tracks are listed and matched songs played, never uploaded.",
        "Lyric backdrop audio analysis" to "'Rhythm' only analyses PCM that ExoPlayer has decoded and is outputting. It does not use the microphone, request recording permission, or store or upload audio and spectra. Analysis stops when playback pauses or you leave the player page.",
        "Lyricon lyric sharing" to "'Send lyrics to Lyricon' is on by default and exposes the current title, artist, lyrics and playback position to other apps through the on-device Lyricon core service; it carries no audio, cookies, tokens or playback URLs. 'Receive lyrics from other players' is off by default; when on, it shows only the song and lyrics Lyricon receives, on this device. Both can be turned off under Settings › Playback; no data passes through a myMusic server.",
        "Writes to the cloud you choose" to "Once sync is on, your playlists, favorites and platform cookies are written to your GitHub Gist, Gitee snippet or WebDAV directory. Cookies are stored in plain text, so never share the file link or directory; the sync file contains no sync tokens, WebDAV passwords, playback URLs or downloaded files. With WebDAV the sync file lives in your WebDAV directory, and the WebDAV password stays on this device.",
        "Network requests" to "The app calls music platform upstream APIs and sync services directly; there is no myMusic relay server. The Soda play signer defaults to SodaM's public signing service (turn it off or use your own instance in settings), and member full-track requests send cookies and full headers to that service. YouTube Music streams directly from YouTube; with the music.op.wiki source, search and audio come from that third party and only track IDs are sent, never cookies. Signing in with a music.op.wiki account sends your username and password only to it, and only the session cookie is kept locally. Third parties still see the usual request details their services need. QR codes for sign-in are generated on this device.",
        "Custom sources" to "LX custom source scripts run only when you import them. Scripts are stored on this device and never synced with your library; a script can reach the network through its own interfaces and receives the title, artist, platform track ID, duration and cover needed to fetch a stream, but it never receives platform cookies stored by myMusic. Online updates re-download the script from its original URL; deleting a source deletes the local script. Import only scripts you trust.",
        "Play reporting" to "With play reporting on (Settings › Play reporting, on by default), the playing track ID, actual listened time (pauses excluded), whether it finished, and the reason for a track change, pause or queue change are sent back to the track's platform so recommendations follow your taste; Soda also includes recently played tracks and durations in its recommendation requests. Supported platforms are Soda Music, NetEase Cloud Music, QQ Music, Kuwo Music and Qianqian Music; Migu and JOOX expose no public reporting interface. Reports contain no usernames, cookies, playback URLs or file contents. When the switch is off no request is sent; records already on the server are not withdrawn.",
        "Diagnostics" to "Copied diagnostics contain only the version, device model, Android version, enabled platforms and recent errors; no cookies, tokens or full playback URLs.",
    )
    SettingsPage(SettingsSection.Privacy, embedded, onBack) {
        itemsIndexed(
            blocks,
            key = { _, item -> "privacy:${item.first}" },
            contentType = { _, _ -> "privacy" },
        ) { index, (title, body) ->
            TextCard(title, body, index, blocks.size, titleColor = MaterialTheme.colorScheme.primary)
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote("Signing out of a platform account or disconnecting sync clears the matching local credentials; uninstalling the app deletes all local data in its directories.")
            }
        }
    }
}
