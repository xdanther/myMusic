package com.mytv.mymusic.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mytv.mymusic.AppLog
import com.mytv.mymusic.data.sync.LocalNetworkAccess
import com.mytv.mymusic.data.sync.SyncStatus
import com.mytv.mymusic.data.sync.SyncUiState
import com.mytv.mymusic.data.webdav.WebDavAccount
import com.mytv.mymusic.data.webdav.WebDavAccountInfo
import com.mytv.mymusic.data.webdav.WebDavPaths
import com.mytv.mymusic.ui.ErrorBlock
import com.mytv.mymusic.ui.InfoNote
import com.mytv.mymusic.ui.MusicActions
import com.mytv.mymusic.ui.MusicChip
import com.mytv.mymusic.ui.components.SwitchRow
import com.mytv.mymusic.ui.components.tvFocusRing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 服务商标签只帮忙填地址、改密码那一栏的名字和提示，不做专门适配（设计稿 ⑫）。
 * 坚果云的地址是固定的，直接填全；Nextcloud / 群晖的地址因人而异，只填协议头，例子写在提示里。
 */
internal enum class WebDavPreset(
    val label: String,
    val url: String,
    val urlHint: String,
    val passwordLabel: String,
    val passwordHint: String,
) {
    Jianguoyun(
        "Nutstore",
        "https://dav.jianguoyun.com/dav/",
        "Nutstore's URL is fixed; no need to change it",
        "App password",
        "For Nutstore use an app password from Security options › Third-party app management, not your login password. Passwords are encrypted with the Android Keystore and kept on this device.",
    ),
    Nextcloud(
        "Nextcloud",
        "https://",
        "Like https://your-domain/remote.php/dav/files/username/",
        "App password",
        "With two-factor auth enabled, generate an app password in Nextcloud Settings › Security. Passwords are encrypted with the Android Keystore and kept on this device.",
    ),
    Synology(
        "Synology",
        "https://",
        "Like https://NAS-address:5006/; install WebDAV Server in Package Center first",
        "Password",
        "Your Synology account password. Passwords are encrypted with the Android Keystore and kept on this device.",
    ),
    Other(
        "Other",
        "",
        "Full WebDAV server URL",
        "Password",
        "Passwords are encrypted with the Android Keystore, kept on this device and never written to the sync file.",
    );

    companion object {
        /** 按已保存的地址猜是哪家，只影响标签和提示。 */
        fun guess(url: String): WebDavPreset {
            val parsed = WebDavPaths.parseServerUrl(url) ?: return if (url.isBlank()) Jianguoyun else Other
            return when {
                parsed.host.endsWith("jianguoyun.com") -> Jianguoyun
                parsed.encodedPath.contains("/remote.php/") -> Nextcloud
                parsed.port == 5005 || parsed.port == 5006 -> Synology
                else -> Other
            }
        }
    }
}

/** 表单状态。密码不进 saveable：转屏、进后台被回收时不把密码写进 Bundle。 */
@Stable
internal class WebDavFormState(
    url: String,
    username: String,
    directory: String,
    trustSelfSigned: Boolean,
    preset: WebDavPreset,
) {
    var url by mutableStateOf(url)
    var username by mutableStateOf(username)
    var password by mutableStateOf("")
    var directory by mutableStateOf(directory)
    var trustSelfSigned by mutableStateOf(trustSelfSigned)
    var preset by mutableStateOf(preset)

    fun selectPreset(next: WebDavPreset) {
        // 只在地址为空或还是别的标签预填的值时替换，不覆盖用户自己填的地址
        if (url.isBlank() || WebDavPreset.entries.any { it.url.isNotEmpty() && it.url == url.trim() }) url = next.url
        preset = next
    }

    fun toAccount() = WebDavAccount(
        serverUrl = url,
        username = username,
        password = password,
        directory = directory,
        trustSelfSigned = trustSelfSigned,
    )

    companion object {
        fun saver() = listSaver<WebDavFormState, Any>(
            save = { listOf(it.url, it.username, it.directory, it.trustSelfSigned, it.preset.name) },
            restore = {
                WebDavFormState(
                    url = it[0] as String,
                    username = it[1] as String,
                    directory = it[2] as String,
                    trustSelfSigned = it[3] as Boolean,
                    preset = WebDavPreset.valueOf(it[4] as String),
                )
            },
        )
    }
}

@Composable
internal fun rememberWebDavFormState(saved: WebDavAccountInfo?): WebDavFormState =
    rememberSaveable(saver = WebDavFormState.saver()) {
        WebDavFormState(
            url = saved?.serverUrl ?: WebDavPreset.Jianguoyun.url,
            username = saved?.username.orEmpty(),
            directory = saved?.directory ?: WebDavPaths.DEFAULT_DIRECTORY,
            trustSelfSigned = saved?.trustSelfSigned ?: false,
            preset = WebDavPreset.guess(saved?.serverUrl.orEmpty()),
        )
    }

/**
 * 只配“上传到 WebDAV”的账号时（设置 › 本地音乐 › WebDAV 服务器）表单的主按钮：换成“保存”，不碰同步位置。
 * 账号还是同一份（WebDavAccountStore），同步以后选 WebDAV 时直接带出来。
 */
internal class WebDavSaveAction(val busy: Boolean, val error: String, val onSave: (WebDavAccount) -> Unit)

/**
 * WebDAV 表单（设计稿 ⑫）：服务商标签、地址、用户名、密码、目录、自签名证书开关，
 * “Test connection”和“Connect and sync”。测试结果写在按钮下方。[save] 不为空时主按钮是“保存”（只给上传用）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WebDavForm(
    form: WebDavFormState,
    sync: SyncUiState,
    actions: MusicActions,
    firstFocus: FocusRequester,
    onConnectAttempt: () -> Unit,
    save: WebDavSaveAction? = null,
) {
    var showPassword by remember { mutableStateOf(false) }
    // 测试时用的密码：改了密码，上次的结果（比如“密码不对”）就不再算数
    var testedPassword by remember { mutableStateOf<String?>(null) }
    val syncing = save?.busy ?: (sync.status == SyncStatus.Syncing)
    val parsedUrl = WebDavPaths.parseServerUrl(form.url)
    val host = parsedUrl?.host.orEmpty()
    val cleartext = parsedUrl?.isHttps == false
    val saved = sync.webDav
    // 地址和用户名都没改时，密码留空就沿用已保存的那个（SyncCoordinator.resolveWebDavDraft）
    val reusePassword = saved != null && saved.hasPassword && form.password.isEmpty() &&
        WebDavPaths.parseServerUrl(saved.serverUrl) == parsedUrl && saved.username == form.username.trim()
    val ready = parsedUrl != null && form.username.isNotBlank() && (form.password.isNotEmpty() || reusePassword)
    // 和 SyncCoordinator.testWebDav 记的 target 一致：位置不含密码，沿用已保存密码时也对得上
    val checkKey = if (ready) form.toAccount().locationKey() else ""
    val check = sync.webDavCheck.takeIf {
        !it.saved && it.target.isNotEmpty() && it.target == checkKey && testedPassword == form.password
    }
    val gate = rememberLocalNetworkGate()
    val lanBlocked = rememberLanBlocked(host)

    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WebDavPreset.entries.forEach { preset ->
            MusicChip(text = preset.label, selected = form.preset == preset, onClick = { form.selectPreset(preset) })
        }
    }
    OutlinedTextField(
        value = form.url,
        onValueChange = { form.url = it.trim() },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(firstFocus).tvFocusRing(RoundedCornerShape(4.dp)),
        singleLine = true,
        label = { Text("Server address") },
        leadingIcon = { Icon(Icons.Filled.Link, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        isError = form.url.isNotBlank() && form.url != form.preset.url && parsedUrl == null,
        supportingText = {
            when {
                cleartext -> Text("This is an http address, so the password travels in plain text. Use https whenever possible.", color = MaterialTheme.colorScheme.tertiary)
                form.url.isNotBlank() && form.url != form.preset.url && parsedUrl == null -> Text("Wrong URL format")
                else -> Text(form.preset.urlHint)
            }
        },
    )
    OutlinedTextField(
        value = form.username,
        onValueChange = { form.username = it },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).tvFocusRing(RoundedCornerShape(4.dp)),
        singleLine = true,
        label = { Text("Username") },
        leadingIcon = { Icon(Icons.Filled.Person, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = form.password,
        onValueChange = { form.password = it },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvFocusRing(RoundedCornerShape(4.dp)),
        singleLine = true,
        label = { Text(form.preset.passwordLabel) },
        placeholder = if (reusePassword) ({ Text("Saved; leave empty to keep it") }) else null,
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    if (showPassword) "Hide password" else "Show password",
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        // 占位文字要点进输入框才看得到，“留空沿用”写在下方提示里
        supportingText = {
            Text(if (reusePassword) "A password is saved; leave empty to keep it. ${form.preset.passwordHint}" else form.preset.passwordHint)
        },
    )
    OutlinedTextField(
        value = form.directory,
        onValueChange = { form.directory = it },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).tvFocusRing(RoundedCornerShape(4.dp)),
        singleLine = true,
        label = { Text("Directory") },
        placeholder = { Text(WebDavPaths.DEFAULT_DIRECTORY) },
        leadingIcon = { Icon(Icons.Filled.Folder, null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        supportingText = { Text("The sync file and uploaded songs live in this directory, created automatically if missing") },
    )
    Box(Modifier.padding(top = 8.dp)) {
        SwitchRow(
            title = "Trust self-signed certificate",
            checked = form.trustSelfSigned,
            onCheckedChange = { form.trustSelfSigned = it },
            index = 0,
            count = 1,
            summary = "Enable only for a server you run yourself",
        )
    }
    if (lanBlocked) {
        Box(Modifier.padding(top = 12.dp)) { LocalNetworkBlockedNote() }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                gate(host) {
                    testedPassword = form.password
                    actions.testWebDav(form.toAccount())
                }
            },
            enabled = ready && check?.running != true && !syncing,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
        ) {
            if (check?.running == true) {
                LoadingIndicator(Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(if (check?.running == true) "Testing…" else "Test connection", maxLines = 1)
        }
        // 测试读到的证书跟着连接一起交上去：用户看过哪张就钉哪张，连接时服务器换了一张会被拦下
        val certificateChanged = check?.certificateChanged == true
        Button(
            onClick = {
                gate(host) {
                    onConnectAttempt()
                    val account = form.toAccount().copy(pinnedCertificate = check?.certificate)
                    if (save != null) save.onSave(account) else actions.connectWebDav(account)
                }
            },
            enabled = ready && !syncing,
            modifier = Modifier.weight(1.3f).heightIn(min = 48.dp).tvFocusRing(RoundedCornerShape(24.dp)),
        ) {
            if (syncing) {
                LoadingIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(if (save != null) "Saving…" else "Connecting…", maxLines = 1)
            } else {
                Icon(Icons.Filled.Cloud, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        save != null && certificateChanged -> "Trust the new certificate and save"
                        save != null -> "Save"
                        certificateChanged -> "Trust the new certificate and connect"
                        else -> "Connect and sync"
                    },
                    maxLines = 1,
                )
            }
        }
    }
    if (check != null && !check.running && check.message.isNotBlank()) {
        Box(Modifier.padding(top = 12.dp)) {
            if (check.ok) InfoNote(check.message, "green") else ErrorBlock(check.message)
        }
    }
    if (save != null) {
        if (save.error.isNotBlank()) Box(Modifier.padding(top = 12.dp)) { ErrorBlock(save.error) }
        return
    }
    if (sync.error.isNotBlank()) {
        Box(Modifier.padding(top = 12.dp)) { ErrorBlock(sync.error) }
    }
    if (sync.initialMerge != null) {
        Box(Modifier.padding(top = 12.dp)) {
            InfoNote("The WebDAV directory already has a sync file and this device has playlists; choose how to merge them the first time.", "amber")
        }
    }
}

/**
 * Android 17 的本地网络保护：WebDAV 填的是 NAS 局域网地址时，没有 ACCESS_LOCAL_NETWORK 连接会被
 * 直接丢包。点按钮时先判断（要解析域名，所以放 IO 线程），需要就申请一次，申请完不论结果都接着
 * 做——被拒时连接会失败，下面的提示会告诉用户去哪里开。
 */
@Composable
private fun rememberLocalNetworkGate(): (String, () -> Unit) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        AppLog.i("WebDavSync", "ACCESS_LOCAL_NETWORK granted=$granted")
        pending?.invoke()
        pending = null
    }
    return remember(launcher) {
        { host, action ->
            if (host.isBlank() || LocalNetworkAccess.isGranted(context)) {
                action()
            } else {
                scope.launch {
                    val local = withContext(Dispatchers.IO) { LocalNetworkAccess.isLocalNetworkHost(host) }
                    if (local) {
                        pending = action
                        launcher.launch(LocalNetworkAccess.PERMISSION)
                    } else {
                        action()
                    }
                }
            }
        }
    }
}

/** 地址指向局域网、又没有本地网络权限时为 true；输入停顿一会儿再解析，不按每个字去查 DNS。 */
@Composable
private fun rememberLanBlocked(host: String): Boolean {
    val context = LocalContext.current
    if (!LocalNetworkAccess.isRequired(context)) return false
    var granted by remember { mutableStateOf(LocalNetworkAccess.isGranted(context)) }
    LifecycleResumeEffect(context) {
        granted = LocalNetworkAccess.isGranted(context)
        onPauseOrDispose { }
    }
    var local by remember(host) { mutableStateOf(false) }
    LaunchedEffect(host, granted) {
        if (granted || host.isBlank()) return@LaunchedEffect
        delay(600)
        local = withContext(Dispatchers.IO) { LocalNetworkAccess.isLocalNetworkHost(host) }
    }
    return !granted && local
}

@Composable
private fun LocalNetworkBlockedNote() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "This server is on your local network. Android 17 needs the nearby devices permission to reach local addresses.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { LocalNetworkAccess.openSettings(context) },
                modifier = Modifier.padding(top = 4.dp).tvFocusRing(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) { Text("Open settings") }
        }
    }
}
