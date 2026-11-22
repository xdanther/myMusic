package com.mytv.mymusic.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytv.mymusic.data.local.LOCAL_ORIGIN_MEDIA
import com.mytv.mymusic.data.local.LocalTitlePolicy
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.components.ConnectedToggleGroup
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SwitchRow
import com.mytv.mymusic.ui.components.ToneIcon
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.local.rememberLocalAccess
import com.mytv.mymusic.ui.local.rememberLocalLibrary
import com.mytv.mymusic.ui.local.rememberWebDavInfo
import com.mytv.mymusic.MyMusicApp
import com.mytv.mymusic.data.cloud.CloudMessages
import com.mytv.mymusic.data.cloud.CloudProvider
import com.mytv.mymusic.data.cloud.CloudQuota
import com.mytv.mymusic.data.local.LocalUploadRules
import com.mytv.mymusic.data.sync.WebDavSyncProvider
import com.mytv.mymusic.data.webdav.WebDavAccountInfo
import com.mytv.mymusic.ui.components.AdaptiveSheet
import com.mytv.mymusic.ui.components.ProviderIcon
import com.mytv.mymusic.ui.components.SegmentGap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.mytv.mymusic.R
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LocalTitlePolicy.label: String
    get() = when (this) {
        LocalTitlePolicy.File -> "File"
        LocalTitlePolicy.FillGaps -> "Fill gaps only"
        LocalTitlePolicy.Match -> "Match result"
    }

/** 设置首页“本地音乐”那一行的当前值。 */
@Composable
internal fun rememberLocalSettingsSummary(): String {
    val library = rememberLocalLibrary() ?: return ""
    val resources = LocalContext.current.resources
    val prefs by library.prefs.collectAsStateWithLifecycle()
    val index by library.index.collectAsStateWithLifecycle()
    val sources = buildList {
        if (prefs.mediaStore && library.hasAudioPermission()) add(resources.getString(R.string.settings_summary_media_store))
        if (prefs.folders.isNotEmpty()) {
            add(
                resources.getQuantityString(
                    R.plurals.settings_summary_local_folders,
                    prefs.folders.size,
                    prefs.folders.size,
                ),
            )
        }
    }
    return when {
        sources.isEmpty() -> resources.getString(R.string.settings_summary_local_none)
        else -> resources.getString(
            R.string.settings_summary_local,
            resources.getQuantityString(
                R.plurals.settings_summary_local_tracks,
                index.items.size,
                index.items.size,
            ),
            sources.joinToString(resources.getString(R.string.settings_join_separator)),
        ) + if (prefs.autoMatch) resources.getString(R.string.settings_summary_local_auto_match) else ""
    }
}

/**
 * 设置 › 本地音乐（设计稿第 ⑩ 张）：扫描范围（System media library、文件夹、跳过短音频）、Cover and lyrics（Automatic matching、
 * Title and artist、同名 .lrc）、上传（默认位置、WebDAV server、加进歌单时提醒、只在 Wi-Fi 下）、
 * 播放（Use the platform version when the file is missing）。全是现有的设置行。
 *
 * [onOpenAccounts] 去平台账号登录网易云；[onConnectWebDav] 去配 WebDAV（同步在用 WebDAV 时是同步那张表单）。
 */
@Composable
internal fun LocalMusicSettingsScreen(
    onBack: () -> Unit,
    embedded: Boolean,
    onOpenAccounts: () -> Unit = {},
    onConnectWebDav: () -> Unit = {},
) {
    val library = rememberLocalLibrary() ?: return
    val prefs by library.prefs.collectAsStateWithLifecycle()
    val index by library.index.collectAsStateWithLifecycle()
    val webDav = rememberWebDavInfo()
    val syncUsesWebDav = rememberSyncUsesWebDav()
    val target = LocalUploadRules.defaultTarget(prefs.uploadTarget, index.clouds)
    val neteaseQuota = rememberNeteaseQuota(CloudProvider.NeteaseCloud in index.clouds)
    var pickTarget by remember { mutableStateOf(false) }
    val access = rememberLocalAccess(library, prefs)
    val first = remember { FocusRequester() }
    if (!embedded) SettingsTvFocus(first)
    val perOrigin = remember(index) { index.items.groupingBy { it.row.origin }.eachCount() }
    val folders = prefs.folders
    val scanCount = 1 + folders.size + (if (access.canPickFolder) 1 else 0) + 1
    SettingsPage(SettingsSection.LocalMusic, embedded, onBack) {
        item(key = "scanHead", contentType = "sectionHeader") { SectionLabel("Scan") }
        item(key = "media", contentType = "switch") {
            val on = prefs.mediaStore && access.hasPermission
            SwitchRow(
                title = "System media library",
                checked = on,
                onCheckedChange = { checked ->
                    // 打开时还没有权限就先要权限；权限结果回来会自动扫描
                    if (checked && !access.hasPermission) access.requestPermission()
                    library.launch { setMediaStore(checked) }
                },
                index = 0,
                count = scanCount,
                modifier = Modifier.focusRequester(first),
                summary = when {
                    !access.hasPermission -> "All audio on this phone; needs the music and audio permission"
                    on -> "All audio on this phone · ${perOrigin[LOCAL_ORIGIN_MEDIA] ?: 0} tracks"
                    else -> "All audio on this phone"
                },
                icon = Icons.Filled.LibraryMusic,
            )
        }
        folders.forEachIndexed { at, folder ->
            item(key = "folder:${folder.uri}", contentType = "folder") {
                ListRow(
                    title = folder.name,
                    index = 1 + at,
                    count = scanCount,
                    onClick = null,
                    icon = Icons.Filled.Folder,
                    summary = "${perOrigin[folder.uri] ?: 0} tracks",
                    showChevron = false,
                    trailing = {
                        IconButton(
                            onClick = { library.launch { removeFolder(folder.uri) } },
                            modifier = Modifier.tvFocusRing(CircleShape),
                        ) {
                            Icon(Icons.Filled.RemoveCircleOutline, "Remove folder '${folder.name}'", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
        }
        // 电视上通常没有系统文件夹选择器，检测不到就不显示
        if (access.canPickFolder) {
            item(key = "addFolder", contentType = "row") {
                ListRow(
                    title = "Add folder",
                    index = 1 + folders.size,
                    count = scanCount,
                    onClick = access.pickFolder,
                    icon = Icons.Filled.CreateNewFolder,
                    summary = "SD card, USB drive or folders the media library doesn't index",
                )
            }
        }
        item(key = "skipShort", contentType = "switch") {
            SwitchRow(
                title = "Skip audio under 30 seconds",
                checked = prefs.skipShort,
                onCheckedChange = { value -> library.launch { updatePrefs { it.copy(skipShort = value) } } },
                index = scanCount - 1,
                count = scanCount,
                summary = "Ringtones, recordings and alerts",
                icon = Icons.Filled.Timer,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            item(key = "lrcNote", contentType = "note") {
                SettingsNote("Since Android 11, tracks in the system media library can't read a neighbouring .lrc; add the folder holding the songs to use your own .lrc.")
            }
        }

        item(key = "matchHead", contentType = "sectionHeader") { SectionLabel("Cover and lyrics") }
        item(key = "autoMatch", contentType = "switch") {
            SwitchRow(
                title = "Automatic matching",
                checked = prefs.autoMatch,
                onCheckedChange = { value -> library.launch { setAutoMatch(value) } },
                index = 0,
                count = 3,
                summary = "Searches by tags first, then a 3-second audio match; files are never modified",
                icon = Icons.Filled.AutoFixHigh,
            )
        }
        item(key = "titlePolicy", contentType = "toggleRow") {
            TitlePolicyRow(prefs.titlePolicy) { value -> library.launch { updatePrefs { it.copy(titlePolicy = value) } } }
        }
        item(key = "preferLrc", contentType = "switch") {
            SwitchRow(
                title = "Prefer a neighbouring .lrc",
                checked = prefs.preferLrc,
                onCheckedChange = { value -> library.launch { updatePrefs { it.copy(preferLrc = value) } } },
                index = 2,
                count = 3,
                summary = "Use a neighbouring .lrc with the same name",
                icon = Icons.Filled.Lyrics,
            )
        }

        item(key = "uploadHead", contentType = "sectionHeader") { SectionLabel("Upload") }
        item(key = "uploadTarget", contentType = "row") {
            val usable = target in index.clouds
            ListRow(
                title = "Upload to",
                index = 0,
                count = 4,
                onClick = { pickTarget = true },
                leading = { UploadTargetIcon(target.takeIf { usable }) },
                summary = when {
                    !usable -> "No location available yet"
                    target == CloudProvider.NeteaseCloud -> listOfNotNull(
                        target.label,
                        neteaseQuota?.let { "${CloudMessages.formatSize(it.used)} / ${CloudMessages.formatSize(it.total)} used" },
                    ).joinToString(" · ")
                    else -> webDav?.let { "WebDAV · ${it.hostLabel}" } ?: "WebDAV"
                },
            )
        }
        item(key = "uploadWebDav", contentType = "row") {
            ListRow(
                title = "WebDAV server",
                index = 1,
                count = 4,
                onClick = onConnectWebDav,
                icon = Icons.Filled.Cloud,
                summary = when {
                    webDav == null || !webDav.hasPassword -> "Not configured · Nutstore, Nextcloud, Synology or your own server"
                    syncUsesWebDav -> "${webDav.hostLabel} · shared with library sync"
                    else -> "${webDav.hostLabel} · for uploaded tracks"
                },
            )
        }
        item(key = "uploadPrompt", contentType = "switch") {
            SwitchRow(
                title = "Remind to upload when adding to a playlist",
                checked = prefs.uploadPrompt,
                onCheckedChange = { value -> library.launch { updatePrefs { it.copy(uploadPrompt = value) } } },
                index = 2,
                count = 4,
                summary = "Warn when a device-only track is added to a synced playlist",
                icon = Icons.Filled.NotificationsActive,
            )
        }
        item(key = "uploadWifi", contentType = "switch") {
            SwitchRow(
                title = "Upload on Wi-Fi only",
                checked = prefs.uploadWifiOnly,
                onCheckedChange = { value -> library.launch { updatePrefs { it.copy(uploadWifiOnly = value) } } },
                index = 3,
                count = 4,
                summary = "Queue on mobile data",
                icon = Icons.Filled.Wifi,
            )
        }

        item(key = "playHead", contentType = "sectionHeader") { SectionLabel("Play") }
        item(key = "fallback", contentType = "switch") {
            SwitchRow(
                title = "Use the platform version when the file is missing",
                checked = prefs.platformFallback,
                onCheckedChange = { value -> library.launch { updatePrefs { it.copy(platformFallback = value) } } },
                index = 0,
                count = 1,
                summary = "For local tracks on other devices that aren't uploaded, play the matched track",
                icon = Icons.Filled.SwapHoriz,
            )
        }
        item(key = "privacy", contentType = "note") {
            Box(Modifier.padding(top = 12.dp)) {
                InfoNote(
                    "Scanning only reads file information and tags, never modifies files and never uploads by itself. Automatic matching sends title and artist to enabled platform searches;" +
                        "When tags are insufficient a 3-second clip is fingerprinted on this device and only the fingerprint goes to NetEase recognition; no audio is sent." +
                        "Files go to your NetEase cloud drive or WebDAV only when you tap upload.",
                )
            }
        }
    }
    if (pickTarget) {
        UploadTargetSheet(
            selected = target,
            configured = index.clouds,
            webDav = webDav,
            onSelect = { value ->
                pickTarget = false
                library.launch { updatePrefs { it.copy(uploadTarget = value.id) } }
            },
            onOpenAccounts = {
                pickTarget = false
                onOpenAccounts()
            },
            onConnectWebDav = {
                pickTarget = false
                onConnectWebDav()
            },
            onDismiss = { pickTarget = false },
        )
    }
}

/** 行首的位置图标：网易云盘用平台图标，WebDAV 用云，No location available yet时是空心云。 */
@Composable
private fun UploadTargetIcon(target: CloudProvider?) {
    when (target) {
        null -> ToneIcon(Icons.Filled.CloudOff, IconTone.Secondary)
        CloudProvider.WebDav -> ToneIcon(Icons.Filled.Cloud, IconTone.Secondary)
        else -> ProviderIcon(id = "netease", name = target.label, size = 40.dp)
    }
}

/**
 * “Upload to”的单选面板：网易云盘、WebDAV 能选；QQ 音乐云盘列出来但不可选（Read only）。
 * 没登录 / 没配置的那行点了去登录 / 去连接（设计稿第 05 节“Upload to”）。
 */
@Composable
private fun UploadTargetSheet(
    selected: CloudProvider,
    configured: Set<CloudProvider>,
    webDav: WebDavAccountInfo?,
    onSelect: (CloudProvider) -> Unit,
    onOpenAccounts: () -> Unit,
    onConnectWebDav: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    SettingsTvFocus(focus)
    AdaptiveSheet(onDismiss = onDismiss, title = "Upload to", subtitle = "Can also be changed in the upload panel") {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
        ) {
            val rows = listOf(CloudProvider.NeteaseCloud, CloudProvider.WebDav, CloudProvider.QqCloud)
            rows.forEachIndexed { at, provider ->
                val ready = provider in configured
                val on = provider == selected && ready
                ListRow(
                    title = provider.label,
                    index = at,
                    count = rows.size,
                    onClick = when {
                        !provider.canUpload -> null
                        !ready && provider == CloudProvider.NeteaseCloud -> onOpenAccounts
                        !ready -> onConnectWebDav
                        else -> ({ onSelect(provider) })
                    },
                    modifier = (if (at == 0) Modifier.focusRequester(focus) else Modifier)
                        .then(if (provider.canUpload) Modifier else Modifier.alpha(0.6f))
                        .semantics {
                            role = Role.RadioButton
                            this.selected = on
                        },
                    summary = when {
                        !provider.canUpload -> "Read only"
                        provider == CloudProvider.NeteaseCloud && !ready -> "Not signed in to NetEase · tap to sign in"
                        provider == CloudProvider.WebDav && !ready -> "Not configured · tap to connect WebDAV"
                        provider == CloudProvider.WebDav -> webDav?.hostLabel.orEmpty()
                        else -> "Tracks already in the cloud upload instantly"
                    },
                    selected = on,
                    showChevron = false,
                    leading = {
                        if (provider == CloudProvider.WebDav) ToneIcon(Icons.Filled.Cloud, IconTone.Secondary)
                        else ProviderIcon(id = provider.id, name = provider.label, size = 40.dp, active = on)
                    },
                    trailing = if (on) ({ Icon(Icons.Filled.Check, null) }) else null,
                )
            }
        }
    }
}

/** 同步是不是正连着 WebDAV：是的话 WebDAV server那行写“和音乐库同步共用”，点进去是同步那张表单。 */
@Composable
private fun rememberSyncUsesWebDav(): Boolean {
    val context = LocalContext.current
    val app = context.applicationContext as? MyMusicApp ?: return false
    val sync by app.sync.state.collectAsStateWithLifecycle()
    return sync.account?.provider == WebDavSyncProvider.ID
}

/** 网易云盘的已用 / 总容量：登录了才查一次（只读列表接口，顺带返回容量）；查不到就不写。 */
@Composable
private fun rememberNeteaseQuota(loggedIn: Boolean): CloudQuota? {
    val context = LocalContext.current
    var quota by remember { mutableStateOf<CloudQuota?>(null) }
    LaunchedEffect(loggedIn) {
        val app = context.applicationContext as? MyMusicApp ?: return@LaunchedEffect
        quota = if (!loggedIn) null else runCatching { withContext(Dispatchers.IO) { app.cloud.netease.quota() } }
            .getOrNull()?.takeIf { it.known }
    }
    return quota
}

/** “Title and artist”：分段列表里的一行，下面挂三段连接按钮组（文件 / Fill gaps only / Match result）。 */
@Composable
private fun TitlePolicyRow(selected: LocalTitlePolicy, onSelect: (LocalTitlePolicy) -> Unit) {
    val shape = segmentShape(1, 3)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToneIcon(Icons.Filled.Title, IconTone.Secondary)
            Column(Modifier.padding(start = 14.dp).semantics(mergeDescendants = true) {}) {
                Text("Title and artist", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "Which to show when file tags and the match disagree",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ConnectedToggleGroup(
            options = LocalTitlePolicy.entries,
            selected = selected,
            onSelect = onSelect,
            label = { it.label },
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 480.dp).fillMaxWidth(),
        )
        Text(
            when (selected) {
                LocalTitlePolicy.File -> "Uses the file's own tags; shows the file name when there is no title."
                LocalTitlePolicy.FillGaps -> "Empty tags or placeholders like Track 01 / Unknown Artist are replaced by the match; decent tags are kept."
                LocalTitlePolicy.Match -> "When matched, use the matched title, artist and album throughout."
            },
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
