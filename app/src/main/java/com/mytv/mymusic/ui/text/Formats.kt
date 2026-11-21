package com.mytv.mymusic.ui.text

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.mytv.mymusic.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 所有界面共用的格式化。语言相关的写法（小数点、月份、复数）都走这里，不要在别处再写死
 * 「M 月 d 日」「亿 / 万」这类中文格式。
 *
 * 语言从参数传进来：API 33+ 的 per-app locale 不会改 [Locale.getDefault]，Compose 里用
 * [currentLocale]，非 Compose 代码用 `resources.configuration.locales[0]`。
 */

/** 当前界面的语言。 */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** 未知时长（<= 0 秒）返回空串。 */
fun formatDurationSeconds(seconds: Int, locale: Locale = Locale.ROOT): String =
    if (seconds <= 0) "" else String.format(locale, "%02d:%02d", seconds / 60, seconds % 60)

/** 播放位置（毫秒）→ mm:ss。 */
fun formatClock(ms: Long, locale: Locale = Locale.ROOT): String =
    String.format(locale, "%02d:%02d", (ms / 60_000).coerceAtLeast(0), ((ms / 1_000) % 60).coerceAtLeast(0))

/** 文件大小。单位本身不翻译，小数点和千位分组跟随语言。 */
fun formatByteSize(bytes: Long, locale: Locale = Locale.ROOT): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(locale, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> String.format(locale, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024L -> String.format(locale, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

/**
 * 播放量等大数：12.3K / 4.5M / 1.2B。
 * 原来是「万 / 亿」，中文用户也看得懂 K/M，换来的是同一套写法在所有语言下都成立。
 */
fun formatCompactCount(count: Long, locale: Locale = Locale.ROOT): String {
    fun oneDecimal(value: Double, suffix: String): String =
        String.format(locale, "%.1f", value).removeSuffix(".0").removeSuffix(",0") + suffix
    return when {
        count >= 1_000_000_000L -> oneDecimal(count / 1_000_000_000.0, "B")
        count >= 1_000_000L -> oneDecimal(count / 1_000_000.0, "M")
        count >= 1_000L -> oneDecimal(count / 1_000.0, "K")
        count > 0 -> count.toString()
        else -> ""
    }
}

/** 月日：中文「10 月 20 日」，其他语言「Oct 20」。 */
fun monthDayPattern(locale: Locale): String = if (locale.language == "zh") "M 月 d 日" else "MMM d"

fun dayFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(monthDayPattern(locale), locale)

/** 24 小时制，与原先的 HH:mm 一致。 */
fun timeFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", locale)

fun formatMonthDay(date: LocalDate, locale: Locale): String = dayFormatter(locale).format(date)

/** 相对时间：刚刚 / N 分钟前 / N 小时前 / N 天前，一周以前退回「MM-dd」。 */
fun relativeTime(resources: Resources, millis: Long, locale: Locale = Locale.ROOT): String {
    if (millis <= 0) return ""
    val delta = System.currentTimeMillis() - millis
    return when {
        delta < 60_000 -> resources.getString(R.string.time_just_now)
        delta < 3_600_000 -> resources.getQuantityString(
            R.plurals.time_minutes_ago,
            (delta / 60_000).toInt(),
            (delta / 60_000).toInt(),
        )
        delta < 86_400_000 -> resources.getQuantityString(
            R.plurals.time_hours_ago,
            (delta / 3_600_000).toInt(),
            (delta / 3_600_000).toInt(),
        )
        delta < 7L * 86_400_000 -> resources.getQuantityString(
            R.plurals.time_days_ago,
            (delta / 86_400_000).toInt(),
            (delta / 86_400_000).toInt(),
        )
        else -> DateTimeFormatter.ofPattern("MM-dd", locale)
            .format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))
    }
}
