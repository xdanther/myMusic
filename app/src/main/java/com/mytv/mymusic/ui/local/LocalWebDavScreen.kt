package com.mytv.mymusic.ui.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.MyMusicApp
import com.mytv.mymusic.data.sync.SyncUiState
import com.mytv.mymusic.data.sync.WebDavSyncProvider
import com.mytv.mymusic.ui.ConfirmDialog
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.components.LocalAppSnackbar
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.PageScaffold
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.library.TvDefaultFocus
import com.mytv.mymusic.ui.library.WebDavForm
import com.mytv.mymusic.ui.library.WebDavSaveAction
import com.mytv.mymusic.ui.library.rememberWebDavFormState
import com.mytv.mymusic.ui.library.withGutter
import kotlinx.coroutines.launch

/** 只给上传用的 WebDAV 账号（设置 › 本地音乐 › WebDAV server）。 */
const val LOCAL_WEBDAV_ROUTE = "localMusic/webdav"

/**
 * 连接 WebDAV 用来放歌（设计稿第 ⑩ 张）：和“音乐库同步 › WebDAV”是同一个表单、同一份账号，只是主按钮
 * 是“保存”，不碰同步位置。同步已经在用 WebDAV 时这里不重复配，直接去同步那边改。
 */
@Composable
fun LocalWebDavScreen(
    sync: SyncUiState,
    actions: MusicActions,
    onBack: () -> Unit,
    onOpenSyncWebDav: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyMusicApp ?: return
    val window = LocalAppWindow.current
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val form = rememberWebDavFormState(sync.webDav)
    val first = remember { FocusRequester() }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    val syncUsesWebDav = sync.account?.provider == WebDavSyncProvider.ID
    val saved = sync.webDav
    TvDefaultFocus(first, syncUsesWebDav)
    PageScaffold(
        title = "WebDAV server",
        subtitle = saved?.let(::webDavTargetLabel) ?: "For uploaded local songs",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding.withGutter(window.gutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (syncUsesWebDav) {
                item(key = "shared", contentType = "note") {
                    Column {
                        InfoNote("Library sync uses this WebDAV account and uploaded songs share the same directory. Change the address or password in library sync.")
                        TextButton(
                            onClick = onOpenSyncWebDav,
                            modifier = Modifier.padding(top = 4.dp).tvFocusRing(RoundedCornerShape(20.dp)),
                        ) { Text("Open library sync") }
                    }
                }
                return@LazyColumn
            }
            item(key = "what", contentType = "note") {
                InfoNote(
                    "Uploaded songs go into music/ under this directory, named by file fingerprint. The WebDAV password is encrypted with the Android Keystore and stays on this device," +
                        "never in the sync file. Choosing WebDAV in library sync later uses this same account.",
                )
            }
            if (window.isTv) {
                item(key = "tv", contentType = "note") {
                    InfoNote("Typing a password on TV is awkward: you can also choose WebDAV in library sync and fill it in by scanning with your phone; it's the same account.")
                }
            }
            item(key = "form", contentType = "form") {
                Column {
                    WebDavForm(
                        form = form,
                        sync = sync,
                        actions = actions,
                        firstFocus = first,
                        onConnectAttempt = {},
                        save = WebDavSaveAction(busy = saving, error = error) { account ->
                            saving = true
                            error = ""
                            scope.launch {
                                val message = app.sync.saveWebDavStorage(account)
                                saving = false
                                if (message.isBlank()) {
                                    // 保存后有排在 WebDAV 上、因为没配置而停下的上传：接着传
                                    app.localUploads.resume(com.mytv.mymusic.data.cloud.CloudProvider.WebDav)
                                    snackbar.showSnackbar("WebDAV saved; uploaded songs go here")
                                    onBack()
                                } else {
                                    error = message
                                }
                            }
                        },
                    )
                }
            }
            if (saved != null) {
                item(key = "remove", contentType = "action") {
                    Box(Modifier.padding(top = 8.dp)) {
                        TextButton(
                            onClick = { confirmRemove = true },
                            modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                        ) { Text("Delete this WebDAV account", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = "Delete the WebDAV account?",
            message = "Only the address and password on this device are deleted. Uploaded songs stay on the server and play normally on other devices.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                scope.launch {
                    app.sync.removeWebDavStorage()
                    snackbar.showSnackbar("WebDAV account deleted")
                    onBack()
                }
            },
            onDismiss = { confirmRemove = false },
        )
    }
}
