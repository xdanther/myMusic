package com.mytv.mymusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mytv.mymusic.data.native.SodaWelfareInfo
import com.mytv.mymusic.ui.components.IconTone
import com.mytv.mymusic.ui.components.ListRow
import com.mytv.mymusic.ui.components.LocalAppWindow
import com.mytv.mymusic.ui.components.PageScaffold
import com.mytv.mymusic.ui.components.SectionLabel
import com.mytv.mymusic.ui.components.SegmentGap
import com.mytv.mymusic.ui.components.tvFocusRing
import com.mytv.mymusic.ui.settings.SettingsNote
import com.mytv.mymusic.ui.SodaWelfareUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 汽水音乐「天天送会员」：活动期内每天打开汽水 App 听歌就能领 1 天Free VIP，
 * 直接领到用户的汽水账号上。任务阈值、领取情况和会员到期都以汽水服务端为准
 * （见 [com.mytv.mymusic.data.native.SodaWelfare]），本机只听歌计时给个参考进度。
 * 未登录汽水时引导去平台账号扫码。
 */
@Composable
fun WelfareScreen(
    soda: SodaWelfareUi,
    todayMinutes: Int,
    onRefresh: () -> Unit,
    onApply: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val window = LocalAppWindow.current
    val vipFocus = remember { FocusRequester() }
    TvDefaultFocus(vipFocus)
    PageScaffold(
        title = "Soda daily VIP",
        subtitle = welfareHeadline(soda, todayMinutes),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !soda.loading,
                modifier = Modifier.tvFocusRing(CircleShape),
            ) {
                Icon(Icons.Filled.Refresh, "Refresh")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding.withGutter(horizontal = window.gutter, extraBottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
        ) {
            when {
                !soda.loggedIn -> item(key = "login", contentType = "card") {
                    WelfareLoginCard(onLogin)
                }
                soda.loading && soda.info.statusCode < 0 -> item(key = "loading", contentType = "card") {
                    WelfareLoadingCard()
                }
                else -> {
                    item(key = "vip", contentType = "vipCard") {
                        WelfareVipCard(soda, vipFocus, onApply)
                    }
                    // 听歌时长任务只有一部分账号有，没配阈值的账号不显示这一段
                    if (soda.info.hasTasks) {
                        item(key = "taskHead", contentType = "section") {
                            SectionLabel("Listening tasks", trailing = {
                                Text(
                                    "Refreshes at midnight",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            })
                        }
                        item(key = "tasks", contentType = "card") { WelfareTaskCard(soda, todayMinutes) }
                    }
                    item(key = "rules", contentType = "card") { WelfareRulesCard(soda.info) }
                    item(key = "note", contentType = "note") {
                        SettingsNote(
                            "Listening time is counted by Soda's servers: plays in this app are reported," +
                                "Listening in the official Soda app counts too. Claimed VIP goes to the signed-in Soda account and works on every device.",
                        )
                    }
                }
            }
            if (soda.error.isNotBlank()) {
                item(key = "error", contentType = "note") {
                    SettingsNote("Couldn't load: ${soda.error}. Tap Refresh in the top-right to retry.")
                }
            }
        }
    }
    // 登录状态变化时重新拉一次；领取情况只有服务端算得准
    LaunchedEffect(soda.loggedIn) { if (soda.loggedIn) onRefresh() }
}

/** 「我的」页入口：Soda daily VIP的状态摘要。 */
@Composable
internal fun WelfareEntryCard(
    soda: SodaWelfareUi,
    todayMinutes: Int,
    onOpen: () -> Unit,
    index: Int = 0,
    count: Int = 1,
    modifier: Modifier = Modifier.padding(top = 12.dp),
) {
    val scheme = MaterialTheme.colorScheme
    ListRow(
        title = "Soda daily VIP",
        index = index,
        count = count,
        onClick = onOpen,
        modifier = modifier,
        icon = Icons.Filled.CardGiftcard,
        iconTone = when {
            !soda.loggedIn -> IconTone.Secondary
            soda.info.vipActive -> IconTone.Primary
            else -> IconTone.Tertiary
        },
        summary = welfareHeadline(soda, todayMinutes),
        trailing = when {
            !soda.loggedIn -> null
            soda.info.vipActive -> {
                { Tag("VIP active", scheme.primaryContainer, scheme.onPrimaryContainer, Icons.Filled.Check) }
            }
            else -> null
        },
    )
}

// ---------------- 卡片 ----------------

@Composable
private fun WelfareVipCard(
    soda: SodaWelfareUi,
    focusRequester: FocusRequester?,
    onApply: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val info = soda.info
    val active = info.vipActive
    val shape = RoundedCornerShape(28.dp)
    val dim = if (active) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
    Surface(
        shape = shape,
        color = if (active) scheme.primaryContainer else scheme.surfaceContainerLow,
        contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(tvCardFocus(shape)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(if (active) scheme.onPrimaryContainer.copy(alpha = 0.12f) else scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CardGiftcard,
                        null,
                        Modifier.size(28.dp),
                        tint = scheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Free VIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (active) "Valid until ${formatVipDate(info.vipExpireAt)}" else "No free VIP today yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dim,
                    )
                }
                if (active) {
                    Tag("Active", scheme.onPrimaryContainer.copy(alpha = 0.12f), scheme.onPrimaryContainer, Icons.Filled.Check)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        info.todayClaimed -> "Already claimed today; come back tomorrow"
                        info.claimable -> "Today's free VIP is unclaimed"
                        !info.activityActive -> "Campaign not running"
                        else -> "Can't claim right now"
                    },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Button(
                    onClick = onApply,
                    enabled = !soda.applying && info.claimable,
                    modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp)),
                ) {
                    if (soda.applying) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
                    } else {
                        Text(if (info.todayClaimed) "Claimed" else "Claim")
                    }
                }
            }
            val period = info.activityPeriod
            if (period.isNotBlank()) {
                Text(
                    "Campaign $period · ${info.appliedCount} days claimed",
                    style = MaterialTheme.typography.bodySmall,
                    color = dim,
                )
            }
            if (!info.activityActive && info.ok) {
                Text(
                    "Soda returned no active daily VIP campaign; it may have ended or your account may not be eligible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = dim,
                )
            }
        }
    }
}

@Composable
private fun WelfareTaskCard(soda: SodaWelfareUi, todayMinutes: Int) {
    val scheme = MaterialTheme.colorScheme
    val info = soda.info
    val minutes = info.taskMinutes
    val shape = RoundedCornerShape(20.dp)
    Surface(shape = shape, color = scheme.surfaceContainerLow, modifier = tvCardFocus(shape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            minutes.forEachIndexed { index, target ->
                val done = index < info.tasksFinished
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(if (done) scheme.primary else scheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = scheme.onPrimary)
                        } else {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Listen for $target min",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (done) "Completed" else "${todayMinutes.coerceAtMost(target)} / $target min on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (index != minutes.lastIndex) {
                    LinearProgressIndicator(
                        progress = { (todayMinutes.toFloat() / target).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        trackColor = scheme.surfaceContainerHighest,
                    )
                }
            }
        }
    }
}

/** Campaign rules：服务端原文，默认收起。 */
@Composable
private fun WelfareRulesCard(info: SodaWelfareInfo) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    var expanded by remember { mutableStateOf(false) }
    Surface(shape = shape, color = scheme.surfaceContainerLow, modifier = tvCardFocus(shape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .tvFocusRing(shape)
                    .focusable()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Campaign rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Listen in the Soda app each day to get a free day of VIP",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (expanded) "Collapse" else "Expand",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                )
            }
            if (expanded && info.rules.isNotBlank()) {
                Text(
                    info.rulesTitle.ifBlank { "Daily VIP rules" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    info.rules,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WelfareLoginCard(onLogin: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Surface(shape = shape, color = scheme.surfaceContainerLow, modifier = tvCardFocus(shape)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(26.dp), tint = scheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text("Sign in to Soda Music first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Free VIP goes to your Soda account; sign in by QR code to view and claim it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onLogin, modifier = Modifier.tvFocusRing(RoundedCornerShape(20.dp))) {
                Text("Sign in by QR code")
            }
        }
    }
}

@Composable
private fun WelfareLoadingCard() {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = scheme.surfaceContainerLow) {
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Text("Reading Soda task status…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ---------------- 工具 ----------------

@Composable
private fun tvCardFocus(shape: Shape): Modifier =
    if (LocalAppWindow.current.isTv) Modifier.tvFocusRing(shape, scale = 1.02f).focusable() else Modifier

/** 入口卡和页头的摘要：VIP 中 > 今日待领 > 已领 > 未登录。 */
internal fun welfareHeadline(soda: SodaWelfareUi, todayMinutes: Int = 0): String {
    val info = soda.info
    return when {
        !soda.loggedIn -> "Scan to sign in to Soda Music to take part"
        soda.loading && info.statusCode < 0 -> "Reading task status…"
        info.vipActive -> "Free VIP valid until ${formatVipDate(info.vipExpireAt)}"
        info.todayClaimed -> "Claimed today; come back tomorrow"
        info.claimable -> "Today's free VIP is unclaimed"
        info.ok && info.hasTasks && !info.allTasksDone ->
            "Today ${info.tasksFinished} / ${info.tasksTotal} tasks, $todayMinutes min on this device"
        info.ok -> "No free VIP to claim right now"
        else -> "Couldn't load; tap to retry"
    }
}

private val vipDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

internal fun formatVipDate(ms: Long): String =
    vipDateFormatter.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
