package com.mytv.mymusic.ui.library

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytv.mymusic.data.sync.InitialMergePreview
import com.mytv.mymusic.data.sync.SyncConflictUi
import com.mytv.mymusic.data.sync.SyncStatus
import com.mytv.mymusic.data.sync.SyncStrategy
import com.mytv.mymusic.data.sync.SyncUiState
import com.mytv.mymusic.data.sync.WebDavSyncProvider
import com.mytv.mymusic.data.webdav.WebDavAccount
import com.mytv.mymusic.data.webdav.WebDavMessages
import com.mytv.mymusic.ui.ConfirmDialog
import com.mytv.mymusic.ui.ErrorBlock
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.adaptive.DialogAvoidsFold
import com.mytv.mymusic.ui.adaptive.FoldRow
import com.mytv.mymusic.ui.adaptive.LocalFoldPane
import com.mytv.mymusic.ui.adaptive.bottom
import com.mytv.mymusic.ui.components.AdaptiveSheet
import com.mytv.mymusic.ui.components.ConnectedToggleGroup
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.LocalOverlayPadding
import com.mytv.mymusic.ui.components.PageScaffold
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SegmentGap
import com.mytv.mymusic.ui.components.SwitchRow
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.relativeTime
import kotlin.coroutines.cancellation.CancellationException

// ======================= 同步中心 =======================

/**
 * Library sync：状态卡片 + 冲突 + Automatic sync开关 + 备份 + 断开。
 * 手机单栏；≥840dp 平板和电视左栏是状态和冲突，右栏是开关和备份。
 * embedded = true 时不带顶栏，直接作为设置双栏的右栏内容（标题由设置页提供）。
 */
@Composable
fun SyncCenterScreen(
    sync: SyncUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    embedded: Boolean = false,
    onConnect: (String) -> Unit,
) {
    val window = LocalAppWindow.current
    var disconnectOpen by remember { mutableStateOf(false) }
    var exportPlaylistsOpen by rememberSaveable { mutableStateOf(false) }
    val exportFile = rememberExportLauncher(actions)
    val importFile = rememberImportLauncher(actions)
    val primaryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { actions.syncIfStaleOnScreen() }
    // 嵌在设置右栏时焦点留在左侧列表，不抢
    if (!embedded) TvDefaultFocus(primaryFocus, sync.account == null)

    val status: LazyListScope.() -> Unit = {
        item(key = "hero", contentType = "hero") {
            if (sync.account == null) {
                SyncIntroCard(onConnect, primaryFocus)
            } else {
                SyncAccountCard(sync, actions, primaryFocus)
            }
        }
        // 已连接卡片上“Test connection”的结果；只认本机保存的那个账号测出来的，表单里草稿的结果不算
        val savedCheck = sync.webDavCheck.takeIf {
            sync.account?.provider == WebDavSyncProvider.ID && it.saved && !it.running && it.message.isNotBlank() &&
                it.target.isNotEmpty() && it.target == savedWebDavKey(sync)
        }
        if (savedCheck != null) {
            item(key = "webDavCheck", contentType = "message") {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedCheck.ok) InfoNote(savedCheck.message, "green") else ErrorBlock(savedCheck.message)
                    // 测出来的证书不是本机记下的那张（换了证书，或者本机还没记过）：用户看过指纹、自己点了才换。
                    // 密码沿用本机保存的（SyncCoordinator.resolveWebDavDraft）
                    val newCertificate = savedCheck.certificate?.takeIf {
                        !it.systemTrusted && it.sha256 != sync.webDav?.certificate?.sha256
                    }
                    if (newCertificate != null) {
                        FilledTonalButton(
                            onClick = {
                                savedWebDavDraft(sync)?.let { actions.connectWebDav(it.copy(pinnedCertificate = newCertificate)) }
                            },
                            enabled = sync.status != SyncStatus.Syncing,
                            modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                        ) { Text(if (savedCheck.certificateChanged) "Trust the new certificate and sync" else "Trust this certificate and sync") }
                    }
                }
            }
        }
        if (sync.status == SyncStatus.Syncing) {
            item(key = "progress", contentType = "progress") {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        sync.phase.ifBlank { "Syncing…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (sync.error.isNotBlank()) {
            item(key = "error", contentType = "error") {
                Box(Modifier.padding(top = 8.dp)) {
                    ErrorBlock(sync.error, onRetry = if (sync.account != null) actions::syncNow else null)
                }
            }
        } else if (sync.message.isNotBlank()) {
            item(key = "message", contentType = "message") {
                Box(Modifier.padding(top = 8.dp)) { InfoNote(sync.message) }
            }
        }
        // 同步文件超过 1 MB：只提示，不拦同步、不删内容
        if (sync.sizeNotice.isNotBlank() && sync.account != null) {
            item(key = "sizeNotice", contentType = "message") {
                Box(Modifier.padding(top = 8.dp)) { InfoNote(sync.sizeNotice, "amber") }
            }
        }
        if (sync.conflicts.isNotEmpty()) {
            item(key = "conflictHead", contentType = "header") { SectionLabel("Pending conflicts", trailing = { CountTag("${sync.conflicts.size}") }) }
            items(sync.conflicts, key = { "conflict:${it.playlistId}" }, contentType = { "conflict" }) { conflict ->
                SyncConflictCard(conflict, actions)
            }
        }
    }
    val settings: LazyListScope.() -> Unit = {
        val account = sync.account
        if (account != null) {
            item(key = "autoHead", contentType = "header") { SectionLabel("Automatic sync") }
            item(key = "auto", contentType = "switch") {
                SwitchRow(
                    title = "Sync after changes",
                    checked = account.autoSync,
                    onCheckedChange = actions::setSyncAutoSync,
                    index = 0,
                    count = 2,
                    icon = Icons.Filled.Sync,
                    summary = "Changes to playlists, favorites or cookies push about 1 second later; opening related pages or returning to the foreground also checks the cloud",
                )
            }
            item(key = "wifi", contentType = "switch") {
                SwitchRow(
                    title = "Sync on Wi-Fi only",
                    checked = account.wifiOnly,
                    onCheckedChange = actions::setSyncWifiOnly,
                    index = 1,
                    count = 2,
                    icon = Icons.Filled.Wifi,
                    summary = "Queues changes on mobile data and uploads once Wi-Fi is back",
                )
            }
        }
        item(key = "backupHead", contentType = "header") { SectionLabel("Backup") }
        item(key = "exportPlaylists", contentType = "row") {
            ListRow(
                title = "Export playlists…",
                index = 0,
                count = 3,
                onClick = { exportPlaylistsOpen = true },
                icon = Icons.Filled.IosShare,
                iconTone = IconTone.Tertiary,
                summary = "Export selected playlists to a file you can share; cookies excluded",
            )
        }
        item(key = "export", contentType = "row") {
            ListRow(
                title = "Export full library backup",
                index = 1,
                count = 3,
                onClick = exportFile,
                icon = Icons.Filled.SaveAlt,
                iconTone = IconTone.Tertiary,
                summary = "Playlists, favorites and platform cookies as JSON, including cookies; for moving to your own device only",
            )
        }
        item(key = "import", contentType = "row") {
            ListRow(
                title = "Import from file",
                index = 2,
                count = 3,
                onClick = importFile,
                icon = Icons.Filled.FileOpen,
                iconTone = IconTone.Tertiary,
                summary = "Reads app playlists, full library backups and LX Music .lxmc files",
            )
        }
        item(key = "providerHead", contentType = "header") { SectionLabel("Sync location") }
        syncLocations.forEachIndexed { index, (id, label) ->
            item(key = "provider:$id", contentType = "row") {
                val current = account != null && account.provider == id
                val webDav = id == WebDavSyncProvider.ID
                ListRow(
                    title = label,
                    index = index,
                    count = syncLocations.size,
                    onClick = { onConnect(id) },
                    icon = if (webDav) Icons.Filled.Dns else Icons.Filled.Cloud,
                    selected = current,
                    summary = when {
                        // WebDAV 写目录里的路径，换过目录的人能一眼看出文件在哪
                        current && webDav -> "In use · ${account.remoteId.ifBlank { account.fileName }}"
                        current -> "In use · ${account.fileName}"
                        webDav -> "Nutstore, Nextcloud, Synology or your own server"
                        account == null -> "Token required"
                        else -> "Use this instead and enter a token"
                    },
                )
            }
        }
        if (account != null) {
            item(key = "disconnect", contentType = "row") {
                ListRow(
                    title = "Disconnect sync",
                    index = 0,
                    count = 1,
                    onClick = { disconnectOpen = true },
                    modifier = Modifier.padding(top = 20.dp),
                    icon = Icons.Filled.CloudOff,
                    iconTone = IconTone.Error,
                    danger = true,
                    summary = if (account.provider == WebDavSyncProvider.ID) {
                        "Delete the WebDAV account on this device; cloud data stays"
                    } else {
                        "Delete the token on this device; cloud data stays"
                    },
                    showChevron = false,
                )
            }
        }
        item(key = "note", contentType = "note") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote("Syncs your playlists, favorites and platform cookies. Cookies are written in plain text into the sync file, so never share the file link or WebDAV directory; recently played, downloads and playback URLs never go to the cloud.")
            }
        }
    }

    val bottom = LocalOverlayPadding.current.calculateBottomPadding() + 24.dp
    when {
        embedded -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
        ) {
            status()
            settings()
        }
        window.twoPane -> PageScaffold(title = "Library sync", subtitle = syncHeadline(sync), onBack = onBack) { padding ->
            TwoColumns(padding, if (window.isTv) 420.dp else 400.dp, status, settings)
        }
        else -> PageScaffold(title = "Library sync", subtitle = syncHeadline(sync), onBack = onBack) { padding ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = padding.withGutter(window.gutter),
                verticalArrangement = Arrangement.spacedBy(SegmentGap),
            ) {
                status()
                settings()
            }
        }
    }

    sync.initialMerge?.let { preview ->
        InitialMergeDialog(preview, sync, actions)
    }
    if (disconnectOpen) {
        ConfirmDialog(
            title = "Disconnect sync",
            message = if (sync.account?.provider == WebDavSyncProvider.ID) {
                "Deletes the WebDAV account, sync baseline and conflict records stored on this device. Local playlists stay, and the sync file on WebDAV is not deleted."
            } else {
                "Deletes the token, sync baseline and conflict records stored on this device. Local playlists stay, and the cloud file is not deleted."
            },
            confirmLabel = "Disconnect",
            danger = true,
            onConfirm = actions::disconnectSync,
            onDismiss = { disconnectOpen = false },
        )
    }
    if (exportPlaylistsOpen) {
        ExportSheet(null, actions, onDismiss = { exportPlaylistsOpen = false })
    }
}

@Composable
internal fun ShapeIcon(icon: ImageVector) {
    // Cookie 形状只做装饰
    Box(
        Modifier
            .size(56.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Sync location：设置里的“Sync location”列表、连接页的切换组都按这个顺序。 */
private val syncLocations = listOf(
    "gist" to "GitHub Gist",
    "gitee" to "Gitee snippet",
    WebDavSyncProvider.ID to "WebDAV",
)

/** 本机保存的 WebDAV 账号对应的“Test connection”结果键，和 SyncCoordinator 记的 target 一致。 */
private fun savedWebDavKey(sync: SyncUiState): String = savedWebDavDraft(sync)?.locationKey().orEmpty()

/** 本机保存的 WebDAV 账号，密码留空（界面状态里没有密码，交给 SyncCoordinator 补上）。 */
private fun savedWebDavDraft(sync: SyncUiState): WebDavAccount? {
    val saved = sync.webDav ?: return null
    return WebDavAccount(
        serverUrl = saved.serverUrl,
        username = saved.username,
        password = "",
        directory = saved.directory,
        trustSelfSigned = saved.trustSelfSigned,
        pinnedCertificate = saved.certificate,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncIntroCard(onConnect: (String) -> Unit, focus: FocusRequester) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            ShapeIcon(Icons.Filled.CloudUpload)
            Text(
                "The cloud is a copy; this device is the primary",
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "After connecting GitHub Gist, Gitee or WebDAV, your playlists sync automatically. Changes save here first and work offline.",
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            // 三个按钮在窄屏一行放不下，自动换行
            FlowRow(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onConnect("gist") },
                    modifier = Modifier.heightIn(min = 48.dp).focusRequester(focus).tvFocusRing(RoundedCornerShape(24.dp)),
                ) { Text("Connect GitHub Gist") }
                FilledTonalButton(
                    onClick = { onConnect("gitee") },
                    modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                ) { Text("Gitee") }
                FilledTonalButton(
                    onClick = { onConnect(WebDavSyncProvider.ID) },
                    modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                ) { Text("WebDAV") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncAccountCard(sync: SyncUiState, actions: MusicActions, focus: FocusRequester) {
    val account = sync.account ?: return
    val uri = LocalUriHandler.current
    val syncing = sync.status == SyncStatus.Syncing
    val title = when {
        syncing -> "Syncing to ${sync.providerLabel}"
        account.lastSyncedAt <= 0 -> "Connected to ${sync.providerLabel}"
        else -> "Synced to ${sync.providerLabel}"
    }
    val detail = listOf(
        relativeTime(account.lastSyncedAt).ifBlank { "Not synced yet" },
        "${sync.playlistCount} playlists",
        "${sync.trackCount} tracks",
        if (sync.favoritesCount > 0) "${sync.favoritesCount} favorites" else "",
        if (sync.cookieCount > 0) "${sync.cookieCount} platform accounts" else "",
        account.accountLabel,
    ).filter { it.isNotBlank() }.joinToString(" · ")
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            ShapeIcon(if (sync.conflicts.isNotEmpty()) Icons.Filled.SyncProblem else Icons.Filled.CloudDone)
            Text(title, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "$detail\nSync playlists, favorites and platform cookies",
                Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            // 本机钉住的自签名证书：电视扫码连接时是电视自己记下的，这里是用户能看到、能核对它的地方
            val certificate = sync.webDav?.certificate?.takeIf { account.provider == WebDavSyncProvider.ID && !it.systemTrusted }
            if (certificate != null) {
                Text(
                    "This device trusts only this certificate: ${WebDavMessages.certificate(certificate)}",
                    Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FlowRow(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = actions::syncNow,
                    enabled = !syncing,
                    modifier = Modifier.heightIn(min = 48.dp).focusRequester(focus).tvFocusRing(RoundedCornerShape(24.dp)),
                ) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (syncing) "Syncing…" else "Sync now")
                }
                if (account.provider == WebDavSyncProvider.ID) {
                    // WebDAV 没有网页可看，第二个按钮换成测本机保存的账号
                    val testing = sync.webDavCheck.running && sync.webDavCheck.saved &&
                        sync.webDavCheck.target == savedWebDavKey(sync)
                    FilledTonalButton(
                        onClick = { actions.testWebDav(null) },
                        enabled = !testing && !syncing,
                        modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                    ) {
                        if (testing) {
                            LoadingIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (testing) "Testing…" else "Test connection")
                    }
                } else if (account.remoteUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { runCatching { uri.openUri(account.remoteUrl) } },
                        modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                    ) { Text(if (account.provider == "gitee") "View snippet" else "View Gist") }
                }
            }
        }
    }
}

/** 冲突提醒：一行说明 + “处理”。处理时手机弹底部面板，大屏弹居中对话框。 */
@Composable
fun SyncConflictCard(conflict: SyncConflictUi, actions: MusicActions) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.SyncProblem, null)
            Column(Modifier.weight(1f)) {
                Text("'${conflict.name}' has a conflict", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Changed locally and in the cloud; nothing uploads until resolved", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { open = true },
                modifier = Modifier.heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text("Resolve") }
        }
    }
    if (open) {
        ConflictResolver(
            title = "'${conflict.name}' changed on both sides",
            detail = "This device: ${conflictSide(conflict.localTracks, conflict.localUpdatedAt, conflict.localDeleted)} · " +
                "Cloud: ${conflictSide(conflict.remoteTracks, conflict.remoteUpdatedAt, conflict.remoteDeleted)}",
            onApply = { strategy -> actions.resolveConflict(conflict.playlistId, strategy) },
            onCancel = { open = false },
            onClose = { open = false },
        )
    }
}

private fun conflictSide(tracks: Int, updatedAt: Long, deleted: Boolean): String =
    if (deleted) "Deleted" else if (updatedAt > 0) "$tracks tracks, changed ${dayLabel(updatedAt)} ${timeLabel(updatedAt)}" else "$tracks tracks"

private val strategyChoices = listOf(
    Triple(SyncStrategy.Merge, "Merge both", "Keep this device's order and append cloud additions (recommended)"),
    Triple(SyncStrategy.Local, "Keep this device", "Overwrite the cloud with this device"),
    Triple(SyncStrategy.Remote, "Use cloud", "Discard local changes"),
)

/** 三选一：选中项满圆角 + secondaryContainer，读屏是单选组。电视默认焦点在推荐项。 */
@Composable
private fun StrategyChoices(selected: SyncStrategy, onSelect: (SyncStrategy) -> Unit, focus: FocusRequester) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(SegmentGap)) {
        strategyChoices.forEachIndexed { index, (strategy, title, summary) ->
            val on = strategy == selected
            val shape = if (on) RoundedCornerShape(16.dp) else segmentShape(index, strategyChoices.size)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .then(if (index == 0) Modifier.focusRequester(focus) else Modifier)
                    .tvFocusRing(shape, scale = 1.02f)
                    .clip(shape)
                    .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(strategy) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = on, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ConflictResolver(
    title: String,
    detail: String,
    onApply: (SyncStrategy) -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    dismissLabel: String = "Cancel",
    dismissible: Boolean = true,
    error: String = "",
) {
    val window = LocalAppWindow.current
    var choice by rememberSaveable { mutableStateOf(SyncStrategy.Merge) }
    val focus = remember { FocusRequester() }
    TvDefaultFocus(focus)
    val apply = {
        onApply(choice)
        onClose()
    }
    if (window.isPhone) {
        // 底部面板被下滑 / 返回时已经收起，若不移出组合，隐藏的面板窗口会挡住整页，所以这里总是走 onCancel
        AdaptiveSheet(onDismiss = onCancel, title = title, subtitle = detail) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                StrategyChoices(choice, { choice = it }, focus)
                if (error.isNotBlank()) {
                    Text(error, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = apply,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 56.dp),
                ) { Text("Apply") }
                if (dismissible && dismissLabel != "Cancel") {
                    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(dismissLabel) }
                }
            }
        }
        return
    }
    // 大屏：居中对话框，宽 440dp；折叠屏书本 / 桌面姿态挪到右页 / 上半屏
    Dialog(onDismissRequest = { if (dismissible) onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogAvoidsFold()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 440.dp).padding(24.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    detail,
                    Modifier.padding(top = 8.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StrategyChoices(choice, { choice = it }, focus)
                if (error.isNotBlank()) {
                    Text(error, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    if (dismissible) {
                        TextButton(onClick = onCancel, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text(dismissLabel) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = apply, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text("Apply") }
                }
            }
        }
    }
}

/** 首次连接时本机和云端都有歌单：同样的三选一。 */
@Composable
private fun InitialMergeDialog(
    preview: InitialMergePreview,
    sync: SyncUiState,
    actions: MusicActions,
    dismissible: Boolean = true,
) {
    val extras = { favorites: Int, cookies: Int ->
        buildString {
            if (favorites > 0) append(" / $favorites favorites")
            if (cookies > 0) append(" / $cookies platform accounts")
        }
    }
    ConflictResolver(
        title = "First merge",
        detail = "This device: ${preview.localPlaylists} playlists / ${preview.localTracks} tracks" +
            extras(preview.localFavorites, preview.localCookies) + " · " +
            "${preview.providerLabel}: ${preview.remotePlaylists} playlists / ${preview.remoteTracks} tracks" +
            extras(preview.remoteFavorites, preview.remoteCookies),
        onApply = actions::resolveInitial,
        // 大屏不可关闭时对话框不会调用它；手机面板收起后必须清掉 initialMerge
        onCancel = actions::cancelInitial,
        // 选择后 initialMerge 清空，对话框自然消失
        onClose = {},
        dismissLabel = "Later",
        dismissible = dismissible,
        error = sync.error,
    )
}

// ======================= Connect sync =======================

@Composable
fun SyncConnectScreen(
    sync: SyncUiState,
    actions: MusicActions,
    initialProvider: String?,
    onBack: () -> Unit,
    onConnected: () -> Unit,
) {
    val window = LocalAppWindow.current
    val uri = LocalUriHandler.current
    var tvManualInput by rememberSaveable { mutableStateOf(false) }
    val useQrSetup = window.isTv && !tvManualInput
    var provider by rememberSaveable {
        mutableStateOf(initialProvider?.takeIf { id -> syncLocations.any { it.first == id } } ?: "gist")
    }
    var token by remember { mutableStateOf("") }
    var remote by rememberSaveable { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val tokenFocus = remember { FocusRequester() }
    val exportFile = rememberExportLauncher(actions)
    val importFile = rememberImportLauncher(actions)
    var exportPlaylistsOpen by rememberSaveable { mutableStateOf(false) }
    val syncing = sync.status == SyncStatus.Syncing
    val gist = provider == "gist"
    val webDav = provider == WebDavSyncProvider.ID
    val webDavForm = rememberWebDavFormState(sync.webDav)

    // 已连接时改用另一处：重试会先把 error 清空，旧 account 还在，不等验证结束就会返回；
    // 连接完成后 account 还会再变，所以只在不在同步中时返回，且只返回一次
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(sync.account, sync.initialMerge, sync.error, syncing) {
        if (!connected && attempted && !syncing && sync.account != null && sync.initialMerge == null && sync.error.isBlank()) {
            connected = true
            // 扫码连接保留成功页，等手机收到结果后再由遥控器确认返回。
            if (!useQrSetup) onConnected()
        }
    }
    val leave = {
        if (sync.initialMerge != null) actions.cancelInitial()
        onBack()
    }
    // 合并方式对话框盖在上面时返回 = 取消First merge并离开；对话框窗口里没有可跟手的画面，手势只在完成后生效
    PredictiveBackHandler(enabled = sync.initialMerge != null) { events ->
        try {
            events.collect { }
            leave()
        } catch (_: CancellationException) {
        }
    }
    if (!useQrSetup) TvDefaultFocus(tokenFocus, provider)

    val providerPicker: @Composable () -> Unit = {
        // 三个选项放不下“Gitee snippet”全名，按钮上写短名，页面副标题仍写全名（设计稿 ⑫）
        ConnectedToggleGroup(
            options = syncLocations.map { it.first },
            selected = provider,
            onSelect = { provider = it },
            label = {
                when (it) {
                    "gist" -> "GitHub Gist"
                    "gitee" -> "Gitee"
                    else -> "WebDAV"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val whatCard: @Composable () -> Unit = {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("What gets synced", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (webDav) {
                        "Your playlists, favorites and platform cookies, written to mymusic-library.json in your WebDAV directory. Cookies are plain text, so do not share this directory. The WebDAV password stays on this device."
                    } else {
                        "Your playlists, favorites and platform cookies. Cookies are written in plain text into the private Gist / snippet, so never share the file link. Recently played, downloads and playback progress are excluded."
                    },
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    val help: @Composable () -> Unit = {
        TextButton(
            onClick = {
                runCatching {
                    uri.openUri(
                        if (gist) "https://github.com/settings/tokens/new?scopes=gist&description=myMusic"
                        else "https://gitee.com/profile/personal_access_tokens",
                    )
                }
            },
            modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
        ) { Text("How do I create a token?") }
    }
    val offline: @Composable () -> Unit = {
        Column {
            Text(
                "No account",
                Modifier.padding(start = 4.dp, top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { exportPlaylistsOpen = true }, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text("Export playlists…") }
                TextButton(onClick = exportFile, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text("Export full library backup") }
                TextButton(onClick = importFile, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) { Text("Import from file") }
            }
        }
    }
    val tokenForm: @Composable ColumnScope.() -> Unit = {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it.trim() },
            modifier = Modifier.fillMaxWidth().focusRequester(tokenFocus).tvFocusRing(RoundedCornerShape(4.dp)),
            singleLine = true,
            label = { Text("Access token") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        if (showToken) "Hide token" else "Show token",
                    )
                }
            },
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            supportingText = {
                Text(
                    if (gist) "Requires the gist scope. The token is encrypted with the Android Keystore, kept on this device and never written to the sync file."
                    else "Enable the gists scope on your Gitee private token. The token is encrypted and kept on this device only.",
                )
            },
        )
        OutlinedTextField(
            value = remote,
            onValueChange = { remote = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvFocusRing(RoundedCornerShape(4.dp)),
            singleLine = true,
            label = { Text(if (gist) "Gist ID (optional)" else "Snippet ID (optional)") },
            placeholder = { Text(if (gist) "Leave empty to create a private Gist" else "Leave empty to create a private snippet") },
            supportingText = { Text("You can also paste an existing link") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        if (window.isTv) {
            Column(Modifier.padding(top = 8.dp)) {
                InfoNote("Scan with your phone to avoid typing a long token on TV.")
                TextButton(
                    onClick = { tvManualInput = false },
                    modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                ) { Text("Configure by QR code") }
            }
        }
        if (sync.error.isNotBlank()) {
            Box(Modifier.padding(top = 12.dp)) { ErrorBlock(sync.error) }
        }
        if (sync.initialMerge != null) {
            Box(Modifier.padding(top = 12.dp)) {
                InfoNote("Both this device and the cloud have playlists; choose how to merge them the first time. The token is encrypted on this device.", "amber")
            }
        }
        Button(
            onClick = {
                attempted = true
                actions.connectSync(provider, token, remote)
            },
            enabled = token.isNotBlank() && !syncing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .heightIn(min = 56.dp)
                .tvFocusRing(RoundedCornerShape(28.dp)),
        ) {
            if (syncing) {
                LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Verifying…")
            } else {
                Icon(Icons.Filled.Cloud, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect and sync")
            }
        }
    }
    val form: @Composable ColumnScope.() -> Unit = {
        if (webDav) {
            WebDavForm(
                form = webDavForm,
                sync = sync,
                actions = actions,
                firstFocus = tokenFocus,
                onConnectAttempt = { attempted = true },
            )
            if (window.isTv) {
                Column(Modifier.padding(top = 8.dp)) {
                    InfoNote("Scan with your phone to avoid typing a password on TV.")
                    TextButton(
                        onClick = { tvManualInput = false },
                        modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                    ) { Text("Configure by QR code") }
                }
            }
        } else {
            tokenForm()
        }
    }

    PageScaffold(
        title = "Connect sync",
        subtitle = when {
            useQrSetup -> "Configure with phone QR code"
            webDav -> "WebDAV"
            gist -> "GitHub Gist"
            else -> "Gitee snippet"
        },
        onBack = leave,
    ) { padding ->
        if (useQrSetup) {
            TvSyncSetupContent(
                sync = sync,
                actions = actions,
                provider = provider,
                connected = connected,
                padding = padding,
                onSubmission = { attempted = true },
                onManualInput = { tvManualInput = true },
                onDone = onConnected,
                infoContent = whatCard,
            )
        } else if (window.twoPane) {
            // 左栏选位置和说明，右栏是表单（最宽 440dp，不拉满）；书本姿态、双竖时两栏以折痕为界
            FoldRow(
                startWidth = 360.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = window.gutter),
                spacing = if (window.isTv) 32.dp else 24.dp,
                start = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(bottom = LocalFoldPane.current.bottom(padding.calculateBottomPadding())),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        providerPicker()
                        whatCard()
                        if (!webDav) help()
                        offline()
                    }
                },
                end = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(bottom = LocalFoldPane.current.bottom(padding.calculateBottomPadding())),
                    ) {
                        Column(Modifier.widthIn(max = 440.dp), content = form)
                    }
                },
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = window.gutter),
            ) {
                Column(Modifier.widthIn(max = 560.dp)) {
                    providerPicker()
                    Spacer(Modifier.size(12.dp))
                    if (webDav) {
                        // 设计稿 ⑫：WebDAV 先说清楚写到哪里、Cookie 是明文，再填表
                        whatCard()
                        Spacer(Modifier.size(12.dp))
                        form()
                        Spacer(Modifier.size(16.dp))
                    } else {
                        Spacer(Modifier.size(4.dp))
                        form()
                        Spacer(Modifier.size(16.dp))
                        whatCard()
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { help() }
                    }
                    offline()
                }
            }
        }
    }

    sync.initialMerge?.let { preview ->
        InitialMergeDialog(preview, sync, actions, dismissible = false)
    }
    if (exportPlaylistsOpen) {
        ExportSheet(null, actions, onDismiss = { exportPlaylistsOpen = false })
    }
}
