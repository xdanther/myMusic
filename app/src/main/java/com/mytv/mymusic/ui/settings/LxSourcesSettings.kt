package com.mytv.mymusic.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mytv.mymusic.data.lx.LxDesktopLibraries
import com.mytv.mymusic.data.lx.LxPlatform
import com.mytv.mymusic.data.lx.LxSourceRecord
import com.mytv.mymusic.data.lx.LxSources
import com.mytv.mymusic.data.lx.MAX_LX_SOURCES
import com.mytv.mymusic.ui.MusicUiState
import com.mytv.mymusic.ui.components.AdaptiveSheet
import com.mytv.mymusic.ui.components.ConnectedToggleGroup
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SegmentedGroup
import com.mytv.mymusic.ui.components.tvFocusRing

/** 音乐源页只留一个入口，不把整套管理器塞进内置平台列表。 */
@Composable
internal fun LxSourcesSettingsEntry(onClick: () -> Unit) {
    val records by LxSources.records.collectAsState()
    ListRow(
        title = "Custom sources",
        index = 0,
        count = 1,
        onClick = onClick,
        icon = Icons.Filled.Code,
        summary = if (records.isEmpty()) {
            "LX scripts · import online or locally"
        } else {
            "${records.count { it.enabled }} enabled · ${records.size} total"
        },
    )
}

@Composable
internal fun LxSourcesSettingsScreen(
    state: MusicUiState,
    onBack: () -> Unit,
    embedded: Boolean = false,
    model: LxSourcesViewModel = viewModel(),
) {
    val records by model.records.collectAsState()
    val operations by model.operations.collectAsState()
    val notice by model.notice.collectAsState()
    var importOpen by rememberSaveable { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var replaceId by remember { mutableStateOf<String?>(null) }
    var deleteFor by remember { mutableStateOf<LxSourceRecord?>(null) }
    val openUri = rememberOpenUri()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val target = replaceId
            model.importLocal(uri, target) {
                importOpen = false
                replaceId = null
            }
        }
    }

    SettingsPage(SettingsSection.LxSources, embedded, onBack) {
        item(key = "add", contentType = "settingRow") {
            ListRow(
                title = "Add custom source",
                index = 0,
                count = 1,
                onClick = if (records.size < MAX_LX_SOURCES) ({ importOpen = true }) else null,
                icon = Icons.Filled.Add,
                iconTone = IconTone.Primary,
                summary = when {
                    records.size >= MAX_LX_SOURCES -> "At most $MAX_LX_SOURCES sources"
                    records.isEmpty() -> "Online URL or local file; you can add several"
                    else -> "Online URL or local file · ${records.size} added"
                },
                showChevron = records.size < MAX_LX_SOURCES,
            )
        }
        if (records.isEmpty()) {
            item(key = "empty", contentType = "note") {
                SettingsNote("No custom sources yet. Add LX scripts to give existing platforms another streaming path without changing search or chart sources.")
            }
        } else {
            item(key = "sourcesHead", contentType = "sectionHeader") {
                SectionLabel("My sources") {
                    Text(
                        "${records.count { it.enabled }} / ${records.size} enabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "sources", contentType = "sourceList") {
                SegmentedGroup {
                    records.forEachIndexed { index, record ->
                        ListRow(
                            title = record.name,
                            index = index,
                            count = records.size,
                            onClick = { selectedId = record.id },
                            icon = Icons.Filled.Code,
                            summary = sourceSummary(record),
                            showChevron = false,
                            trailing = {
                                Switch(
                                    checked = record.enabled,
                                    onCheckedChange = { model.setEnabled(record, !record.enabled) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "${if (record.enabled) "Off" else "Enable"}${record.name}"
                                    },
                                )
                            },
                        )
                    }
                }
            }
            item(key = "sourcesNote", contentType = "note") {
                SettingsNote("This order only applies to scripts inside the single LX Music source. If every script fails, fallback continues per playback and source settings; tap a source to test, update, reorder or delete it.")
            }
        }
        operations.message.takeIf { it.isNotBlank() }?.let { message ->
            item(key = "message", contentType = "note") {
                ListRow(
                    title = message,
                    index = 0,
                    count = 1,
                    onClick = model::clearMessage,
                    summary = "Tap to dismiss",
                    showChevron = false,
                )
            }
        }
    }

    val selected = records.firstOrNull { it.id == selectedId }
    selected?.let { record ->
        LxSourceDetailSheet(
            record = record,
            index = records.indexOfFirst { it.id == record.id },
            count = records.size,
            testing = "test:${record.id}" in operations.busy,
            updating = "update:${record.id}" in operations.busy,
            testResult = operations.tests[record.id].orEmpty(),
            error = operations.errors["test:${record.id}"]
                ?: operations.errors["update:${record.id}"]
                ?: operations.errors["toggle:${record.id}"]
                ?: operations.errors["move:${record.id}"],
            onDismiss = { selectedId = null },
            onTest = { model.test(record, state.current, state.cookies) },
            onUpdate = { model.update(record) },
            onReplace = {
                replaceId = record.id
                filePicker.launch(arrayOf("*/*"))
            },
            onMove = { model.move(record, it) },
            onDelete = { deleteFor = record },
            onOpenHomepage = { if (record.homepage.isNotBlank()) openUri(record.homepage) },
        )
    }
    if (importOpen) {
        LxImportSheet(
            busy = "import" in operations.busy,
            error = operations.errors["import"].orEmpty(),
            onDismiss = { if ("import" !in operations.busy) importOpen = false },
            onImport = { url -> model.importOnline(url) { importOpen = false } },
            onFile = {
                replaceId = null
                filePicker.launch(arrayOf("*/*"))
            },
            onOpenCatalog = { openUri("https://github.com/pdone/lx-music-source") },
        )
    }
    deleteFor?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Delete ${record.name}?") },
            text = { Text("Deletes the script stored on this device; existing songs and playlists stay, and streaming falls back to built-in interfaces.") },
            confirmButton = {
                TextButton(onClick = {
                    model.delete(record)
                    selectedId = null
                    deleteFor = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancel") } },
        )
    }
    notice?.let { update ->
        AlertDialog(
            onDismissRequest = model::dismissNotice,
            title = { Text("${update.name} has an update") },
            text = { Text(update.log, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                if (update.updateUrl.startsWith("http://") || update.updateUrl.startsWith("https://")) {
                    TextButton(onClick = {
                        openUri(update.updateUrl)
                        model.dismissNotice()
                    }) { Text("View update") }
                } else {
                    TextButton(onClick = model::dismissNotice) { Text("Got it") }
                }
            },
            dismissButton = if (update.updateUrl.isNotBlank()) {
                { TextButton(onClick = model::dismissNotice) { Text("Off") } }
            } else null,
        )
    }
}

@Composable
private fun LxSourceDetailSheet(
    record: LxSourceRecord,
    index: Int,
    count: Int,
    testing: Boolean,
    updating: Boolean,
    testResult: String,
    error: String?,
    onDismiss: () -> Unit,
    onTest: () -> Unit,
    onUpdate: () -> Unit,
    onReplace: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onOpenHomepage: () -> Unit,
) {
    AdaptiveSheet(onDismiss = onDismiss, title = record.name, subtitle = sourceSubtitle(record)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (record.desktop) {
                SettingsNote(
                    "Desktop plugin: used only to fetch streams; the script asks its own endpoint for a URL by title / id." +
                        "The required axios and he runtimes are downloaded from the official CDN, verified with sha256 and cached on this device;" +
                        "A script is still executable code that can reach any HTTP address; import only trusted sources.",
                )
            }
            if (record.sourceUrl.isBlank()) {
                SettingsNote("Locally imported scripts have no download URL, so their contents sync with the rest to your Gist / Gitee / WebDAV;" +
                    "Without sync it exists only on this device; uninstalling or clearing data removes it and you must import it again.")
            }
            if (record.description.isNotBlank()) {
                Text(
                    record.description,
                    Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SegmentedGroup {
                ListRow(
                    title = "Supported platforms and qualities",
                    index = 0,
                    count = if (record.homepage.isBlank()) 1 else 2,
                    onClick = null,
                    icon = Icons.Filled.Tune,
                    summary = sourceCapabilities(record),
                    showChevron = false,
                )
                if (record.homepage.isNotBlank()) {
                    ListRow(
                        title = "Script home page",
                        index = 1,
                        count = 2,
                        onClick = onOpenHomepage,
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        summary = record.homepage.substringAfter("://").substringBefore('/'),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SegmentedGroup {
                ListRow(
                    title = "Test streaming",
                    index = 0,
                    count = 4,
                    onClick = if (testing) null else onTest,
                    icon = Icons.Filled.PlayArrow,
                    iconTone = IconTone.Primary,
                    summary = if (testing) "Running the script and reading media…" else "Checks the script and a real media response",
                    showChevron = false,
                    trailing = if (testing) {
                        { LoadingIndicator(Modifier.size(24.dp)) }
                    } else null,
                )
                ListRow(
                    title = if (record.sourceUrl.isBlank()) "Replace script" else "Update script",
                    index = 1,
                    count = 4,
                    onClick = if (updating) null else if (record.sourceUrl.isBlank()) onReplace else onUpdate,
                    icon = if (record.sourceUrl.isBlank()) Icons.Filled.FileOpen else Icons.Filled.Refresh,
                    summary = if (updating) "Downloading and verifying…" else if (record.sourceUrl.isBlank()) "Choose a new local file" else "Download the latest version from the original URL",
                    showChevron = false,
                    trailing = if (updating) {
                        { LoadingIndicator(Modifier.size(24.dp)) }
                    } else null,
                )
                ListRow(
                    title = "Reorder",
                    index = 2,
                    count = 4,
                    onClick = null,
                    icon = Icons.Filled.SwapVert,
                    summary = "Position ${index + 1} of $count sources",
                    showChevron = false,
                    trailing = {
                        IconButton(onClick = { onMove(-1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Move ${record.name} up")
                        }
                        IconButton(onClick = { onMove(1) }, enabled = index < count - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Move ${record.name} down")
                        }
                    },
                )
                ListRow(
                    title = "Delete source",
                    index = 3,
                    count = 4,
                    onClick = onDelete,
                    icon = Icons.Filled.DeleteOutline,
                    iconTone = IconTone.Error,
                    summary = "Removes the local script only; songs and playlists are unaffected",
                    danger = true,
                    showChevron = false,
                )
            }
            if (testResult.isNotBlank()) {
                Text(
                    testResult,
                    Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!error.isNullOrBlank()) {
                Text(
                    error,
                    Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun LxImportSheet(
    busy: Boolean,
    error: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onFile: () -> Unit,
    onOpenCatalog: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf("online") }
    var url by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    AdaptiveSheet(onDismiss = onDismiss, title = "Add custom source", subtitle = "Runs LX Music mobile JavaScript scripts") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            ConnectedToggleGroup(
                options = listOf("online", "local"),
                selected = mode,
                onSelect = { mode = it },
                label = { if (it == "online") "Online URL" else "Local file" },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            if (mode == "online") {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Script URL") },
                    placeholder = { Text("https://…/latest.js") },
                    singleLine = true,
                    enabled = !busy,
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.let { url = it } }, enabled = !busy) {
                            Icon(Icons.Filled.ContentPaste, "Paste the script URL")
                        }
                    },
                )
                TextButton(onClick = onOpenCatalog, enabled = !busy, modifier = Modifier.align(Alignment.End)) {
                    Text("Open the pdone source collection")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                }
                Button(
                    onClick = { onImport(url) },
                    enabled = url.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).tvFocusRing(),
                ) {
                    if (busy) LoadingIndicator(Modifier.size(20.dp))
                    Text(if (busy) "Downloading and verifying…" else "Download and import", Modifier.padding(start = 8.dp))
                }
            } else {
                ListRow(
                    title = if (busy) "Reading and verifying…" else "Choose a .js script",
                    index = 0,
                    count = 1,
                    onClick = if (busy) null else onFile,
                    icon = Icons.Filled.FileOpen,
                    summary = "UTF-8 · up to 2 MB",
                    showChevron = !busy,
                    trailing = if (busy) {
                        { LoadingIndicator(Modifier.size(24.dp)) }
                    } else null,
                )
            }
            if (error.isNotBlank()) {
                Text(
                    error,
                    Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SettingsNote(
                "Import only scripts you trust. Scripts can reach the network but never receive platform cookies stored by myMusic. A failed verification saves nothing." +
                    "Mobile scripts run directly; desktop (Electron) plugins need " + LxDesktopLibraries.summary() +
                    " are downloaded from the official CDN, verified one by one with sha256 and cached on this device.",
            )
        }
    }
}

private fun sourceSubtitle(record: LxSourceRecord): String = listOf(
    record.version.takeIf { it.isNotBlank() }?.let { "v$it" }.orEmpty(),
    record.author,
    if (record.desktop) "Desktop plugin" else if (record.sourceUrl.isBlank()) "Local script" else "Online script",
).filter { it.isNotBlank() }.joinToString(" · ")

private fun sourceSummary(record: LxSourceRecord): String = listOf(
    sourceSubtitle(record),
    sourcePlatforms(record),
).filter { it.isNotBlank() }.joinToString("\n")

private fun sourceCapabilities(record: LxSourceRecord): String {
    val qualities = record.capabilities.values.flatMap { it.qualities }.distinct()
    return listOf(
        sourcePlatforms(record),
        qualities.takeIf { it.isNotEmpty() }?.joinToString(" · ", prefix = "Quality: ") { qualityLabel(it) }.orEmpty(),
    ).filter { it.isNotBlank() }.joinToString("\n")
}

private fun sourcePlatforms(record: LxSourceRecord): String = LxPlatform.labels.mapNotNull { (source, label) ->
    val capability = record.capabilities[source] ?: return@mapNotNull null
    if ("musicUrl" in capability.actions) label else null
}.joinToString(" · ")

private fun qualityLabel(quality: String): String = when (quality) {
    "flac" -> "FLAC"
    "flac24bit" -> "24bit"
    "hires" -> "Hi-Res"
    "atmos" -> "Atmos"
    "atmos_plus" -> "Atmos+"
    "master" -> "Master"
    "sq" -> "Lossless"
    else -> quality
}
