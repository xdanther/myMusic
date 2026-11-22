package com.mytv.mymusic.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.CrashReport
import com.mytv.mymusic.CrashReporter
import com.mytv.mymusic.ui.adaptive.DialogAvoidsFold
import com.mytv.mymusic.ui.components.LocalAppSnackbar
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.tvFocusRing
import kotlinx.coroutines.launch

/** 启动时提示和关于页复查共用；链接不含报告正文，复制与打开 GitHub 都是用户主动操作。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrashReportDialog(
    report: CrashReport,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboard.current
    val snackbar = LocalAppSnackbar.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tv = LocalAppWindow.current.isTv
    val copyFocus = remember { FocusRequester() }
    val lines = remember(report.text) { report.text.lines() }
    val buttonShape = RoundedCornerShape(20.dp)

    LaunchedEffect(tv) {
        if (tv) {
            withFrameNanos { }
            runCatching { copyFocus.requestFocus() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            DialogAvoidsFold()
            Text(if (onDelete == null) "Last unexpected exit" else "Crash report")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "The report records uncaught Java/Kotlin exceptions, stays on this device and is never uploaded automatically; native crashes don't pass through this handler.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(report.summary, style = MaterialTheme.typography.bodySmall)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    // 按行懒加载，长堆栈和日志不会在一个 Text 里一次性排版；复制始终使用完整原文。
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        items(lines.size) { index ->
                            Text(
                                lines[index],
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                Text(
                    "The GitHub link only pre-fills a short device/version summary. Copy the full report first, open Issues and paste it into the body; check for personal information before sending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("myMusic crash report", report.text)))
                                snackbar.showSnackbar("Full crash report copied")
                            }
                        },
                        modifier = Modifier.focusRequester(copyFocus).tvFocusRing(buttonShape),
                    ) {
                        Text("Copy full report")
                    }
                    TextButton(
                        onClick = {
                            runCatching { uriHandler.openUri(CrashReporter.issueUrl(context, report.summary)) }
                                .onFailure { scope.launch { snackbar.showSnackbar("No app on this device can open links") } }
                        },
                        modifier = Modifier.tvFocusRing(buttonShape),
                    ) {
                        Text("Open GitHub Issues")
                    }
                    if (onDelete != null) {
                        TextButton(onClick = onDelete, modifier = Modifier.tvFocusRing(buttonShape)) {
                            Text("Delete report", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing(buttonShape)) {
                Text("Close")
            }
        },
    )
}
