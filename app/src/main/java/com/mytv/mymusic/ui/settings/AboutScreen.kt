package com.mytv.mymusic.ui.settings

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.R
import com.mytv.mymusic.CrashReport
import com.mytv.mymusic.CrashReporter
import com.mytv.mymusic.ui.text.appVersion
import com.mytv.mymusic.ui.CrashReportDialog
import com.mytv.mymusic.ui.ErrorBlock
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.MusicUiState
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.data.GithubMirror
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.LocalAppSnackbar
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.SegmentedGroup
import com.mytv.mymusic.ui.components.tvFocusRing
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROJECT_URL = "https://github.com/mytv-android/myMusic"
private const val TELEGRAM_URL = "https://t.me/mytv_android_channel"

/** App info, crash report entry, privacy/licenses links and diagnostics. */
@Composable
fun AboutScreen(
    state: MusicUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    onPrivacy: () -> Unit,
    embedded: Boolean = false,
) {
    val openUri = rememberOpenUri()
    val clipboard = LocalClipboard.current
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMirrorPicker by remember { mutableStateOf(false) }
    var crashReport by remember { mutableStateOf<CrashReport?>(null) }
    var crashReportLoaded by remember { mutableStateOf(false) }
    var showCrashReport by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        crashReport = withContext(Dispatchers.IO) { CrashReporter.read(context) }
        crashReportLoaded = true
    }
    val (versionName, versionCode) = remember(context) { appVersion(context) }

    SettingsPage(SettingsSection.About, embedded, onBack, subtitle = "Version ${versionName.ifBlank { "1.0" }} (build $versionCode)") {
        item(key = "head", contentType = "appHeader") { AppHeader(versionName, versionCode) }
        item(key = "rows", contentType = "settingsGroup") {
            SegmentedGroup(Modifier.padding(top = 12.dp)) {
                ListRow(
                    title = "Report an issue",
                    index = 0,
                    count = 8,
                    onClick = {
                        if (crashReportLoaded) {
                            if (crashReport != null) showCrashReport = true
                            else openUri(CrashReporter.issueUrl(context))
                        }
                    },
                    icon = Icons.Filled.BugReport,
                    summary = when {
                        !crashReportLoaded -> "Checking for crash reports on this device…"
                        crashReport != null -> "A crash report is available to view, copy or delete"
                        else -> "Open GitHub Issues with a short device and version note"
                    },
                )
                ListRow(
                    title = "GitHub mirror",
                    index = 1,
                    count = 8,
                    onClick = { showMirrorPicker = true },
                    icon = Icons.Filled.CloudDownload,
                    summary = when (state.githubMirror) {
                        GithubMirror.Auto -> "Test all routes and use the fastest"
                        GithubMirror.Direct -> "Connect to github.com directly"
                        else -> "Prefer ${state.githubMirror.label}, fall back in order"
                    },
                )
                ListRow(
                    title = "Privacy",
                    index = 2,
                    count = 8,
                    onClick = onPrivacy,
                    icon = Icons.Filled.PrivacyTip,
                    summary = "What stays on this device and what leaves it",
                )
                ListRow(
                    title = "Open source licenses",
                    index = 3,
                    count = 8,
                    onClick = onLicenses,
                    icon = Icons.Filled.Code,
                    summary = "AGPL-3.0 · music-lib, Folia modules, Rush and third-party dependencies",
                )
                ListRow(
                    title = "Project home",
                    index = 4,
                    count = 8,
                    onClick = { openUri(PROJECT_URL) },
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    summary = "github.com/mytv-android/myMusic",
                    showChevron = false,
                )
                ListRow(
                    title = "Telegram group",
                    index = 5,
                    count = 8,
                    onClick = { openUri(TELEGRAM_URL) },
                    icon = Icons.Filled.Group,
                    summary = "t.me/mytv_android_channel",
                    showChevron = false,
                )
                ListRow(
                    title = "Copy diagnostics",
                    index = 6,
                    count = 8,
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("myMusic diagnostics", actions.diagnostics())))
                            snackbar.showSnackbar("Diagnostics copied")
                        }
                    },
                    icon = Icons.Filled.ContentCopy,
                    iconTone = IconTone.Tertiary,
                    summary = "Device, version, enabled platforms and the last ${com.mytv.mymusic.AppLog.snapshot().size} log lines",
                    showChevron = false,
                )
                ListRow(
                    title = "Export log file",
                    index = 7,
                    count = 8,
                    onClick = {
                        scope.launch { snackbar.showSnackbar(actions.exportLog()) }
                    },
                    icon = Icons.Filled.Share,
                    iconTone = IconTone.Tertiary,
                    summary = "Share the run log as a file, handy when there are many lines",
                    showChevron = false,
                )
            }
        }
        item(key = "sodam", contentType = "creditCard") {
            Box(Modifier.padding(top = 12.dp)) {
                SodamCredit(note = "Full-track signing for Soda member songs comes from SodaM (AGPL-3.0)")
            }
        }
        item(key = "logNote", contentType = "infoNote") {
            Box(Modifier.padding(top = 8.dp)) {
                InfoNote("Run logs live in this device's memory only, at most 800 lines, and are cleared when the app exits. Cookies and tokens are stripped before writing and filtered again before export, but still send them only to people you trust.")
            }
        }
        item(key = "links", contentType = "linksCard") { LinksCard(openUri) }
        item(key = "note", contentType = "infoNote") {
            Box(Modifier.padding(top = 16.dp)) {
                InfoNote("Cookies, play history and downloads stay on this device. Sync only uses the Gist / Gitee account or WebDAV server you choose, and writes your own playlist data; the app has no central server.")
            }
        }
        item(key = "footer", contentType = "footer") {
            Text(
                "Platform layer music-lib / 7cc4467b\nA learning project; follow each music platform's terms of service.",
                Modifier.fillMaxWidth().padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
    if (showMirrorPicker) {
        GithubMirrorDialog(
            selected = state.githubMirror,
            onPick = {
                showMirrorPicker = false
                scope.launch { actions.setGithubMirror(it) }
            },
            onDismiss = { showMirrorPicker = false },
        )
    }
    crashReport?.takeIf { showCrashReport }?.let { report ->
        CrashReportDialog(
            report = report,
            onDismiss = { showCrashReport = false },
            onDelete = {
                scope.launch {
                    val deleted = withContext(Dispatchers.IO) { CrashReporter.delete(context) }
                    if (deleted) {
                        crashReport = null
                        showCrashReport = false
                    }
                    snackbar.showSnackbar(if (deleted) "Crash report deleted" else "Delete failed, please try again")
                }
            },
        )
    }
}

/** Mirror picker, each row followed by a short note about what that route can do. */
@Composable
private fun GithubMirrorDialog(
    selected: GithubMirror,
    onPick: (GithubMirror) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub mirror") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GithubMirror.entries.forEach { mirror ->
                    val summary = when (mirror) {
                        GithubMirror.Auto -> "Race all routes and use the fastest reachable one"
                        GithubMirror.Direct -> "No proxy; api.github.com may rate-limit requests"
                        GithubMirror.ProxyOrg, GithubMirror.ProxyCom -> "Works for GitHub pages and downloads"
                        else -> "Speeds up downloads only; other routes handle GitHub pages"
                    }
                    ListRow(
                        title = mirror.label,
                        index = GithubMirror.entries.indexOf(mirror),
                        count = GithubMirror.entries.size,
                        onClick = { onPick(mirror) },
                        summary = summary,
                        selected = mirror == selected,
                        showChevron = false,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/**
 * Project and group QR codes, generated on-device without an external image service.
 * Phones open the link on tap; TVs often have no browser, so scan with a phone instead.
 */
@Composable
private fun LinksCard(openUri: (String) -> Unit) {
    val window = LocalAppWindow.current
    // 上限按设备给，实际尺寸跟着格子宽度收，窄屏上也不会顶出格外
    val qrSize = when {
        window.isTv -> 240.dp
        window.isPhone -> 150.dp
        else -> 200.dp
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
    ) {
        Text("Scan to visit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Tap to open the link, or scan the QR code with your phone.",
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 两个格子按二维码上限收窄，宽屏上不会各自摊成一片空白
        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = qrSize * 2 + 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LinkQrCell("Project home", "github.com/mytv-android/myMusic", PROJECT_URL, qrSize, Modifier.weight(1f), openUri)
                LinkQrCell("Telegram group", "t.me/mytv_android_channel", TELEGRAM_URL, qrSize, Modifier.weight(1f), openUri)
            }
        }
    }
}

/** One QR cell: dark code on a light background so scanners can read it, independent of the theme. */
@Composable
private fun LinkQrCell(
    title: String,
    host: String,
    url: String,
    qrSize: Dp,
    modifier: Modifier = Modifier,
    openUri: (String) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .tvFocusRing(shape, scale = 1.02f)
            .clickable { openUri(url) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .sizeIn(maxWidth = qrSize, maxHeight = qrSize)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            QrCodeImage(url, Modifier.fillMaxSize(), description = "$title QR code")
        }
        Text(
            title,
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            host,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Same gradient as res/drawable/ic_launcher_background.xml (top-left to bottom-right). */
private val LauncherIconBackground = Brush.linearGradient(
    listOf(Color(0xFF1E9483), Color(0xFF006B5F), Color(0xFF00524A)),
)

@Composable
private fun AppHeader(versionName: String, versionCode: Long) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 桌面图标原样：自适应图标的背景渐变 + 前景（屏幕框和音符）。前景画布 108dp、可见区 72dp，
        // 所以前景按 84 × 108 / 72 = 126dp 画，超出部分被圆角裁掉，和启动器里看到的比例一致
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(LauncherIconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(126.dp))
        }
        Text("myMusic", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineSmall)
        Text(
            "Version ${versionName.ifBlank { "1.0" }} (build $versionCode) · AGPL-3.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
