package com.mytv.mymusic.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.mytv.mymusic.R
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.LocalAppSnackbar
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.LocalOverlayPadding
import com.mytv.mymusic.ui.components.PageScaffold
import com.mytv.mymusic.ui.components.ProviderIcon
import com.mytv.mymusic.ui.components.SegmentGap
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing
import kotlinx.coroutines.launch

/** 设置分区。“开源许可 / 隐私说明”挂在“关于与更新”下面，双栏时左栏仍高亮“关于与更新”。 */
internal enum class SettingsSection(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val tone: IconTone,
) {
    Appearance("settings/appearance", R.string.settings_section_appearance, R.string.settings_section_appearance_desc, Icons.Filled.Palette, IconTone.Primary),
    // 原来叫“歌词”；枚举名和路由保持不变，旧的深链接和保存的选中项继续有效
    Lyrics("settings/lyrics", R.string.settings_section_lyrics, R.string.settings_section_lyrics_desc, Icons.Filled.PlayCircle, IconTone.Primary),
    Playback("settings/playback", R.string.settings_section_playback, R.string.settings_section_playback_desc, Icons.Filled.SwapHoriz, IconTone.Primary),
    Report("settings/report", R.string.settings_section_report, R.string.settings_section_report_desc, Icons.Filled.Analytics, IconTone.Primary),
    Equalizer("settings/equalizer", R.string.settings_section_equalizer, R.string.settings_section_equalizer_desc, Icons.Filled.GraphicEq, IconTone.Primary),
    Sources("settings/sources", R.string.settings_section_sources, R.string.settings_section_sources_desc, Icons.Filled.Tune, IconTone.Primary),
    LxSources("settings/sources/custom", R.string.settings_section_custom_sources, R.string.settings_section_custom_sources_desc, Icons.Filled.Code, IconTone.Secondary),
    Search("settings/search", R.string.settings_section_search, R.string.settings_section_search_desc, Icons.Filled.Search, IconTone.Primary),
    Swipe("settings/swipe", R.string.settings_section_swipe, R.string.settings_section_swipe_desc, Icons.Filled.Swipe, IconTone.Secondary),
    Accounts("settings/accounts", R.string.settings_section_accounts, R.string.settings_section_accounts_desc, Icons.Filled.Person, IconTone.Secondary),
    Sync("settings/sync", R.string.settings_section_sync, R.string.settings_section_sync_desc, Icons.Filled.Cloud, IconTone.Secondary),
    LocalMusic("settings/local", R.string.settings_section_local, R.string.settings_section_local_desc, Icons.Filled.AudioFile, IconTone.Secondary),
    Storage("settings/storage", R.string.settings_section_storage, R.string.settings_section_storage_desc, Icons.Filled.Storage, IconTone.Tertiary),
    About("settings/about", R.string.settings_section_about, R.string.settings_section_about_desc, Icons.Filled.Info, IconTone.Tertiary),
    Licenses("settings/licenses", R.string.settings_section_licenses, R.string.settings_section_licenses_desc, Icons.Filled.Code, IconTone.Tertiary),
    Privacy("settings/privacy", R.string.settings_section_privacy, R.string.settings_section_privacy_desc, Icons.Filled.PrivacyTip, IconTone.Tertiary),
    ;

    val parent: SettingsSection? get() = when (this) {
        Licenses, Privacy -> About
        LxSources -> Sources
        else -> null
    }

    companion object {
        /** 设置首页 / 双栏左栏的三组。 */
        val groups = listOf(
            listOf(Appearance, Lyrics, Playback, Report, Equalizer, Sources, Search, Swipe),
            listOf(Accounts, Sync, LocalMusic),
            listOf(Storage, About),
        )

        /** 实际列出的分组：电视上没有滑动操作，不列「滑动操作」。 */
        fun groupsFor(tv: Boolean): List<List<SettingsSection>> =
            if (tv) groups.map { group -> group.filter { it != Swipe } } else groups
    }
}

/**
 * 设置子页骨架。手机和 <840dp 平板是独立页面（Large app bar + 返回）；
 * 双栏时作为右栏内容，标题由双栏外壳画，这里只排列表，左右留白也由外壳给。
 */
@Composable
internal fun SettingsPage(
    section: SettingsSection,
    embedded: Boolean,
    onBack: () -> Unit,
    subtitle: String = stringResource(section.subtitleRes),
    content: LazyListScope.() -> Unit,
) {
    if (embedded) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalOverlayPadding.current.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
            content = content,
        )
        return
    }
    val gutter = LocalAppWindow.current.gutter
    PageScaffold(title = stringResource(section.titleRes), subtitle = subtitle, onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
            content = content,
        )
    }
}

/** 列表下方的说明文字：不是卡片，左缩进与卡片内文字对齐。 */
@Composable
internal fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 平台头像：有平台图标就用图标，认不出才退回圆形底 + 简称。已登录 / 已启用时用 primary 描边标出。 */
@Composable
internal fun ProviderMark(name: String, size: Dp = 40.dp, active: Boolean = false, id: String? = null) {
    ProviderIcon(id = id, name = name, size = size, active = active)
}

/**
 * 纯文字的分段卡片（许可、隐私说明）。电视上可以获得焦点，
 * 否则整页没有可聚焦元素，遥控器无法滚动。
 */
@Composable
internal fun TextCard(
    title: String,
    body: String,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    footer: @Composable (() -> Unit)? = null,
) {
    val shape = segmentShape(index, count)
    val tv = LocalAppWindow.current.isTv
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .tvFocusRing(shape, scale = 1.02f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (tv && footer == null) Modifier.focusable() else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = titleColor)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        footer?.invoke()
    }
}

/** 打开外部链接。电视上常常没有浏览器，打不开时用 Snackbar 说明，而不是崩溃。 */
@Composable
internal fun rememberOpenUri(): (String) -> Unit {
    val handler = LocalUriHandler.current
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    return remember(handler, snackbar, scope) {
        { uri ->
            runCatching { handler.openUri(uri) }.onFailure {
                scope.launch { snackbar.showSnackbar("No app on this device can open links") }
            }
        }
    }
}

/** 电视默认焦点：等一帧让列表排好版再请求，目标还没挂上时静默忽略。 */
@Composable
internal fun SettingsTvFocus(requester: FocusRequester, key: Any? = Unit) {
    if (!LocalAppWindow.current.isTv) return
    LaunchedEffect(key) {
        withFrameNanos { }
        runCatching { requester.requestFocus() }
    }
}

/** SodaM：签名服务协议、公共签名服务与官方图标的来源。 */
internal const val SODAM_URL = "https://github.com/sodahub-org/sodam"

/**
 * SodaM 官方图标（`crates/sodam/assets/brand/sodam-logo.svg`）。
 * 图标本身自带深青圆角底，只做圆角裁剪，不再套底色。
 */
@Composable
internal fun SodamLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_sodam_logo),
        contentDescription = "SodaM",
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.26f)),
    )
}

/**
 * “Powered by SodaM”署名条：图标 + 一句说明，点一下打开 SodaM 仓库。
 * 公共签名服务会把带 Cookie 的请求头发给对方，署名要让用户知道流量去了哪里。
 */
@Composable
internal fun SodamCredit(modifier: Modifier = Modifier, logoSize: Dp = 44.dp, note: String? = null) {
    val openUri = rememberOpenUri()
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .tvFocusRing(shape, scale = 1.02f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { openUri(SODAM_URL) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SodamLogo(logoSize)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Powered by SodaM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                note ?: "Signing for Soda member full tracks comes from SodaM (AGPL-3.0)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
