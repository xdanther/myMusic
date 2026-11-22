package com.mytv.mymusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.data.Playlist
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.PlatformImportUi
import com.mytv.mymusic.ui.components.AdaptiveSheet
import com.mytv.mymusic.ui.adaptive.DialogAvoidsFold
import com.mytv.mymusic.ui.components.AppLoading
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.ProviderIcon
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SheetDismissGuard
import com.mytv.mymusic.ui.components.segmentShape
import com.mytv.mymusic.ui.components.tvFocusRing

/**
 * 登录成功后'把这些歌单导入成绑定歌单'（设计稿 docs/platform-import.html）。
 *
 * 外壳挂在 [com.mytv.mymusic.ui.AppNavigation]，从哪一页登录都盖在上面。导入的每一条都走
 * [MusicActions] 的绑定流程，所以之后平台增删歌会跟着更新，和从链接导入一致。
 */
@Composable
fun PlatformImportSheet(state: PlatformImportUi, actions: MusicActions) {
    if (!state.open) return
    val tv = LocalAppWindow.current.isTv
    val tagline = if (state.providerName.isBlank()) "" else "Signed in to ${state.providerName}"
    var confirmLeave by remember { mutableStateOf(false) }
    // 导入中下滑、点外面、按返回都先问一句：已经写进去的照留，剩下的取消
    val guard = remember(state.applying) {
        if (state.applying == null) null else SheetDismissGuard(
            blocked = { true },
            onBlocked = { confirmLeave = true },
        )
    }
    AdaptiveSheet(
        onDismiss = { actions.dismissPlatformImport() },
        title = "Import your playlists",
        subtitle = if (tagline.isBlank()) "Selected playlists follow platform updates" else "$tagline · selected playlists follow platform updates",
        dismissGuard = guard,
    ) {
        when {
            state.loading -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { AppLoading(text = "Reading playlists…") }
            state.error.isNotBlank() -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = { actions.openPlatformImport(state.provider) },
                    modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                ) { Text("Retry") }
            }
            state.playlists.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "This account has no playlists to import.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> PlatformImportBody(state, actions, tv)
        }
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { DialogAvoidsFold(); Text("Import still running") },
            text = {
                Text(
                    "Playlists already imported are kept; the rest are cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    actions.dismissPlatformImport()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep importing") } },
        )
    }
}

@Composable
private fun ColumnScope.PlatformImportBody(state: PlatformImportUi, actions: MusicActions, tv: Boolean) {
    val selectable = remember(state.playlists, state.boundIds) {
        state.playlists.filter { it.id !in state.boundIds }
    }
    var selected by remember(state.playlists, state.boundIds) {
        mutableStateOf(selectable.map { it.id }.toSet())
    }
    val chosen = state.playlists.filter { it.id in selected }
    val applying = state.applying
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Ticked playlists follow platform updates: additions and removals on the platform are applied here. Already linked ones are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        SectionLabel(
            "${state.providerName.ifBlank { "Platform" }} · ${state.playlists.size} playlists",
            trailing = {
                TextButton(
                    onClick = {
                        selected = if (selected.size == selectable.size) emptySet() else selectable.map { it.id }.toSet()
                    },
                    enabled = applying == null && selectable.isNotEmpty(),
                    modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                ) { Text(if (selected.size == selectable.size && selectable.isNotEmpty()) "Select none" else "Select all") }
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.playlists.forEachIndexed { index, playlist ->
                val bound = playlist.id in state.boundIds
                val checked = playlist.id in selected
                PlatformImportRow(
                    playlist = playlist,
                    checked = checked,
                    bound = bound,
                    dimmed = applying != null,
                    merges = playlist.id in state.mergeableNames,
                    provider = state.provider,
                    index = index,
                    count = state.playlists.size,
                    enabled = applying == null && !bound,
                    modifier = if (index == 0 && !bound) Modifier.sheetDefaultFocus(tv) else Modifier,
                    onToggle = {
                        selected = if (checked) selected - playlist.id else selected + playlist.id
                    },
                )
            }
        }
        Spacer(Modifier.size(8.dp))
    }
    Button(
        onClick = { actions.applyPlatformImport(chosen.map { it.id }) },
        enabled = applying == null && chosen.isNotEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .heightIn(min = 52.dp)
            .tvFocusRing(RoundedCornerShape(26.dp)),
    ) {
        Text(
            when {
                applying != null -> "Importing ${applying.first + 1}/${applying.second}…"
                chosen.isEmpty() -> "No playlists selected"
                else -> "Import ${chosen.size} playlists · ${chosen.sumOf { it.trackCount }} tracks"
            },
            style = MaterialTheme.typography.titleSmall,
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = { actions.dismissPlatformImport() },
            modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
        ) { Text("Later") }
        TextButton(
            onClick = { actions.dismissPlatformImport(forever = true) },
            modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
        ) { Text("Don't ask again for this platform") }
    }
}

/** 一行歌单：勾选框 + 名字 + 首数；已绑定的灰掉并标'已绑定'。整行点一下切换勾选。 */
@Composable
private fun PlatformImportRow(
    playlist: Playlist,
    checked: Boolean,
    bound: Boolean,
    /** 导入中整行压暗、不可点。 */
    dimmed: Boolean,
    merges: Boolean,
    provider: String,
    index: Int,
    count: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val shape = segmentShape(index, count)
    val on = checked && !bound
    Row(
        modifier
            .fillMaxWidth()
            // 已绑定的只是勾不了，字要照常读得清；整屏压暗只在导入中
            .then(if (dimmed) Modifier.alpha(0.5f) else Modifier)
            .tvFocusRing(shape, scale = 1.02f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier)
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(on)
            }
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(checked = on, onCheckedChange = null, enabled = enabled && !dimmed)
        }
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    playlist.name,
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${playlist.trackCount} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (bound) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                "Linked",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            Text(
                when {
                    bound -> "Already here; won't be imported twice"
                    merges -> "Merges into the playlist with the same name"
                    else -> "Follow platform updates after importing"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .clearAndSetSemantics { contentDescription = platformLabel(provider) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProviderIcon(id = provider, name = platformLabel(provider), size = 22.dp, corner = 7.dp)
                Text(
                    shortPlatform(provider),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
