package com.mytv.mymusic.ui.settings

import android.content.res.Resources
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.R
import com.mytv.mymusic.aggregate.MatchStrictness
import com.mytv.mymusic.aggregate.PlaybackMode
import com.mytv.mymusic.data.SwipeAction
import com.mytv.mymusic.data.SwipePrefs
import com.mytv.mymusic.data.sync.SyncUiState
import com.mytv.mymusic.data.update.UpdateUiState
import com.mytv.mymusic.playback.EqualizerUiState
import com.mytv.mymusic.ui.MusicUiState
import com.mytv.mymusic.ui.adaptive.FoldRow
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.LocalOverlayPadding
import com.mytv.mymusic.ui.components.PageScaffold
import com.mytv.mymusic.ui.components.SegmentedGroup
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.formatBytes
import com.mytv.mymusic.ui.providerName
import com.mytv.mymusic.ui.text.relativeTime
import com.mytv.mymusic.ui.theme.FontScale
import com.mytv.mymusic.ui.theme.LayoutMode
import kotlin.coroutines.cancellation.CancellationException

/** 设置列表每项下方显示的当前值，以及“关于”上的新版本标记。 */
@Immutable
internal data class SettingsOverview(
    val values: Map<SettingsSection, String>,
    val updateAvailable: Boolean,
)

internal fun settingsOverview(
    resources: Resources,
    state: MusicUiState,
    sync: SyncUiState,
    update: UpdateUiState,
    equalizer: EqualizerUiState,
    cacheBytes: Long,
    downloadsBytes: Long,
    localMusic: String = "",
    rtl: Boolean = false,
): SettingsOverview {
    val appearance = state.appearance
    val on = resources.getString(R.string.settings_summary_on)
    val off = resources.getString(R.string.settings_summary_off)
    val separator = resources.getString(R.string.settings_join_separator)
    val logged = state.providers.filter { !state.cookies[it.id].isNullOrBlank() }
    val account = sync.account
    val info = update.info
    val updateAvailable = info != null
    val version = resources.getString(
        R.string.settings_summary_version,
        update.currentVersionName.ifBlank { "1.0" },
        update.currentVersionCode,
    )
    return SettingsOverview(
        values = mapOf(
            SettingsSection.Appearance to buildString {
                append(
                    resources.getString(
                        R.string.settings_summary_appearance,
                        resources.getString(appearance.colorSource.labelRes),
                        resources.getString(appearance.browseStyle.labelRes),
                        themeLabel(resources, appearance.theme),
                    ),
                )
                if (appearance.layout != LayoutMode.Auto) {
                    append(resources.getString(R.string.settings_summary_suffix, resources.getString(appearance.layout.labelRes)))
                }
                if (appearance.fontScale != FontScale.Standard) {
                    append(
                        resources.getString(
                            R.string.settings_summary_suffix,
                            resources.getString(
                                R.string.settings_summary_font_scale,
                                resources.getString(appearance.fontScale.labelRes),
                            ),
                        ),
                    )
                }
            },
            // 歌词动画的具体名称还没逐个搬进资源，先只写封面和歌词背景；动画那一行跟着播放页一起迁移
            SettingsSection.Lyrics to resources.getString(
                R.string.settings_summary_lyrics,
                resources.coverShapeLabel(state.coverShape),
                resources.lyricsBackgroundLabel(state.lyricsBackground),
            ),
            SettingsSection.Playback to playbackModeSummary(resources, state.aggregation.playbackMode) +
                if (state.aggregation.matchStrictness == MatchStrictness.Strict) {
                    resources.getString(
                        R.string.settings_summary_suffix,
                        resources.getString(R.string.settings_summary_strict),
                    )
                } else {
                    ""
                },
            SettingsSection.Report to if (state.playReportEnabled) on else off,
            SettingsSection.Equalizer to resources.getString(
                R.string.settings_summary_eq,
                resources.getString(eqPresetName(equalizer.presetId)),
                if (equalizer.enabled) on else off,
            ),
            SettingsSection.Sources to sourceSummary(resources, state),
            SettingsSection.Search to resources.getString(
                R.string.settings_summary_default_source,
                resources.providerBrand(state.providerId, providerName(state.providers, state.providerId)),
            ),
            SettingsSection.Swipe to swipeSummaryText(resources, state.swipe, rtl),
            SettingsSection.Accounts to when {
                logged.isEmpty() -> resources.getString(R.string.settings_summary_accounts_none)
                logged.size <= 2 -> resources.getString(
                    R.string.settings_summary_accounts_named,
                    logged.joinToString(separator) { resources.providerBrand(it.id, it.name) },
                )
                else -> resources.getQuantityString(
                    R.plurals.settings_summary_accounts_count,
                    logged.size,
                    logged.size,
                )
            },
            SettingsSection.Sync to if (account != null) {
                resources.getString(
                    R.string.settings_summary_sync,
                    sync.providerLabel,
                    relativeTime(resources, account.lastSyncedAt).ifBlank {
                        resources.getString(R.string.settings_summary_never_synced)
                    },
                )
            } else {
                resources.getString(R.string.settings_summary_sync_off)
            },
            SettingsSection.LocalMusic to localMusic,
            SettingsSection.Storage to resources.getString(
                R.string.settings_summary_storage,
                formatBytes(cacheBytes),
                formatBytes(downloadsBytes),
            ),
            SettingsSection.About to if (updateAvailable) {
                resources.getString(
                    R.string.settings_summary_update,
                    version,
                    info?.versionName.orEmpty(),
                    info?.versionCode ?: 0,
                )
            } else {
                version
            },
        ),
        updateAvailable = updateAvailable,
    )
}

internal fun themeLabel(resources: Resources, theme: String): String = resources.getString(
    when (theme) {
        "light" -> R.string.settings_summary_theme_light
        "dark" -> R.string.settings_summary_theme_dark
        else -> R.string.settings_summary_theme_system
    },
)

private fun playbackModeSummary(resources: Resources, mode: PlaybackMode): String = resources.getString(
    when (mode) {
        PlaybackMode.Auto -> R.string.settings_summary_playback_auto
        PlaybackMode.Priority -> R.string.settings_summary_playback_priority
        PlaybackMode.Pinned -> R.string.settings_summary_playback_pinned
    },
)

/** 音乐源的当前值：启用几个、第一个是谁。顺序就是用户排的顺序。 */
private fun sourceSummary(resources: Resources, state: MusicUiState): String {
    val searchable = state.providers.filter { it.search }
    val enabled = state.aggregation.enabledOrDefault(searchable.map { it.id })
    val first = enabled.firstOrNull()?.let { id -> resources.providerBrand(id, providerName(state.providers, id)) }
    return when {
        enabled.isEmpty() -> resources.getString(R.string.settings_summary_all_off)
        first.isNullOrBlank() -> resources.getQuantityString(
            R.plurals.settings_summary_platforms,
            enabled.size,
            enabled.size,
        )
        else -> resources.getQuantityString(
            R.plurals.settings_summary_platforms_first,
            enabled.size,
            enabled.size,
            first,
        )
    }
}

/** 滑动操作的摘要：只写起始那一侧（向右滑），关着写「已关闭」。 */
private fun swipeSummaryText(resources: Resources, prefs: SwipePrefs, rtl: Boolean): String {
    if (!prefs.enabled) return resources.getString(R.string.settings_summary_swipe_off)
    val side = resources.getString(
        if (rtl) R.string.settings_summary_swipe_side_end else R.string.settings_summary_swipe_side_start,
    )
    val buttons = prefs.start
        .map { resources.swipeActionLabel(it) }
        .joinToString(resources.getString(R.string.settings_join_separator))
        .ifBlank { resources.getString(R.string.settings_summary_swipe_no_buttons) }
    val full = prefs.startFull
        .takeIf { it != SwipeAction.None }
        ?.let { resources.getString(R.string.settings_summary_swipe_full, resources.swipeActionLabel(it)) }
        .orEmpty()
    return resources.getString(R.string.settings_summary_swipe, side, buttons, full)
}

/** 手机（和 <840dp 平板）的设置首页：三组分段列表，每项下方是当前值，点进各自的子页。 */
@Composable
internal fun SettingsHomeScreen(
    overview: SettingsOverview,
    onOpen: (SettingsSection) -> Unit,
    onBack: (() -> Unit)?,
) {
    val gutter = LocalAppWindow.current.gutter
    PageScaffold(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item(key = "groups", contentType = "settingsGroups") { SettingsGroups(overview, selected = null, twoPane = false, onOpen = onOpen) }
            item(key = "note", contentType = "note") {
                SettingsNote(stringResource(R.string.settings_sync_note), Modifier.padding(top = 8.dp))
            }
        }
    }
}

/**
 * ≥840dp 平板和电视：左边设置列表、右边当前分区，不跳页。
 * 电视上焦点在左栏移动时右栏跟着换；在右栏按返回键先回到左栏。
 */
@Composable
internal fun SettingsTwoPane(
    overview: SettingsOverview,
    initial: SettingsSection,
    detail: @Composable (section: SettingsSection, open: (SettingsSection) -> Unit) -> Unit,
) {
    val window = LocalAppWindow.current
    val tv = window.isTv
    var selected by rememberSaveable(initial) { mutableStateOf(initial) }
    val listed = selected.parent ?: selected
    val requesters = remember { SettingsSection.groups.flatten().associateWith { FocusRequester() } }
    val backFocus = remember { FocusRequester() }
    var detailFocused by remember { mutableStateOf(false) }
    val bottom = LocalOverlayPadding.current.calculateBottomPadding()

    fun focusList(section: SettingsSection) {
        if (tv) runCatching { requesters.getValue(section).requestFocus() }
    }

    // 电视：进来时焦点落在选中项上（直接打开设置时就是第一项“外观”）
    if (tv) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            focusList(listed)
        }
    }
    // 打开“开源许可 / 隐私说明”后原来获得焦点的行已经不在了，把焦点交给右栏的返回键
    LaunchedEffect(selected) {
        if (tv && selected.parent != null) {
            withFrameNanos { }
            runCatching { backFocus.requestFocus() }
        }
    }
    // 预测性返回：在“开源许可 / 隐私说明”这类子分区上，手势过程中右栏内容跟手右移淡出，松手才退回上一级
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = selected.parent != null) { events ->
        try {
            events.collect { event -> backProgress = event.progress }
            selected.parent?.let { parent ->
                selected = parent
                focusList(parent)
            }
        } catch (_: CancellationException) {
        } finally {
            backProgress = 0f
        }
    }
    // 电视：焦点在右栏时返回先回到左栏列表，没有手势，按键返回直接完成
    PredictiveBackHandler(enabled = tv && detailFocused && selected.parent == null) { events ->
        try {
            events.collect { }
            focusList(listed)
        } catch (_: CancellationException) {
        }
    }

    // 书本姿态、双竖：设置列表收到折痕为止，分区内容在折痕另一侧；双横：列表在上屏、内容在下屏
    FoldRow(
        startWidth = if (tv) 320.dp else 380.dp,
        modifier = Modifier.fillMaxSize().then(if (tv) Modifier else Modifier.statusBarsPadding()),
        start = {
            Column(
                Modifier
                    .fillMaxHeight()
                    // 从右栏按左键回来时回到刚才的选中项，而不是同一高度的别的项（电视上那会切走右栏）
                    .focusRestorer()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = if (tv) 28.dp else 24.dp,
                        end = 16.dp,
                        top = if (tv) 36.dp else 24.dp,
                        bottom = bottom + 24.dp,
                    ),
            ) {
                Text(
                    stringResource(R.string.settings_title),
                    Modifier.padding(start = 4.dp, bottom = 16.dp),
                    style = if (tv) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
                SettingsGroups(
                    overview = overview,
                    selected = listed,
                    twoPane = true,
                    onOpen = { selected = it },
                    itemModifier = { section ->
                        Modifier
                            .focusRequester(requesters.getValue(section))
                            .onFocusChanged { if (tv && it.isFocused && listed != section) selected = section }
                    },
                )
            }
        },
        end = {
            Column(
                Modifier
                    .fillMaxHeight()
                    .then(
                        if (tv) {
                            Modifier.padding(start = 8.dp, end = 48.dp, top = 36.dp)
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(start = 32.dp, end = 32.dp, top = 24.dp)
                        },
                    )
                    .onFocusChanged { detailFocused = it.hasFocus }
                    // 右侧内容跟着手势右移淡出，面板底色不动；进度只重绘图层，不触发重组
                    .graphicsLayer {
                        alpha = 1f - backProgress
                        translationX = size.width * 0.2f * backProgress
                    },
            ) {
                DetailHeader(
                    section = selected,
                    onBack = selected.parent?.let { parent -> { selected = parent; focusList(parent) } },
                    backFocus = backFocus,
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    key(selected) { detail(selected) { selected = it } }
                }
            }
        },
    )
}

@Composable
private fun DetailHeader(section: SettingsSection, onBack: (() -> Unit)?, backFocus: FocusRequester) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocus).tvFocusRing(CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(
                        R.string.settings_back_to,
                        section.parent?.let { stringResource(it.titleRes) }.orEmpty(),
                    ),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                stringResource(section.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(section.subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 三组分段列表。双栏时选中项高亮、不显示箭头。 */
@Composable
private fun SettingsGroups(
    overview: SettingsOverview,
    selected: SettingsSection?,
    twoPane: Boolean,
    onOpen: (SettingsSection) -> Unit,
    itemModifier: (SettingsSection) -> Modifier = { Modifier },
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection.groupsFor(LocalAppWindow.current.isTv).forEach { group ->
            SegmentedGroup {
                group.forEachIndexed { index, section ->
                    val badge = section == SettingsSection.About && overview.updateAvailable
                    ListRow(
                        title = stringResource(section.titleRes),
                        index = index,
                        count = group.size,
                        onClick = { onOpen(section) },
                        modifier = itemModifier(section),
                        icon = section.icon,
                        iconTone = section.tone,
                        summary = overview.values[section].orEmpty(),
                        selected = section == selected,
                        showChevron = !twoPane,
                        trailing = if (badge) {
                            {
                                NewBadge()
                                if (!twoPane) Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewBadge() {
    Box(
        Modifier
            .heightIn(min = 20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 7.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.settings_new_badge),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onError,
        )
    }
}

