package com.mytv.mymusic.ui.text

import android.content.res.Resources
import com.mytv.mymusic.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 所有界面共用的格式化。应用只有一套英文文案，所以默认统一用 [Locale.ENGLISH]：
 * 设备是中文时也不会再冒出「10 月 20 日」或「亿 / 万」。
 */

private val English = Locale.ENGLISH

/** 未知时长（<= 0 秒）返回空串。 */
fun formatDurationSeconds(seconds: Int, locale: Locale = English): String =
    if (seconds <= 0) "" else String.format(locale, "%02d:%02d", seconds / 60, seconds % 60)

/** 播放位置（毫秒）→ mm:ss。 */
fun formatClock(ms: Long, locale: Locale = English): String =
    String.format(locale, "%02d:%02d", (ms / 60_000).coerceAtLeast(0), ((ms / 1_000) % 60).coerceAtLeast(0))

/** 文件大小。单位不翻译，小数点和千位分组跟随 [locale]。 */
fun formatByteSize(bytes: Long, locale: Locale = English): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(locale, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> String.format(locale, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024L -> String.format(locale, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

/**
 * 播放量等大数：12.3K / 4.5M / 1.2B。
 * 原来是「万 / 亿」，中文用户也看得懂 K/M，换来的是同一套写法。
 */
fun formatCompactCount(count: Long, locale: Locale = English): String {
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

/** 月日：Oct 20。界面只有英文，不再跟随设备语言。 */
fun dayFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", English)

/** 24 小时制，与原先的 HH:mm 一致。 */
fun timeFormatter(locale: Locale = English): DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", locale)

fun formatMonthDay(date: LocalDate): String = dayFormatter().format(date)

/** 相对时间：Just now / N minutes ago / N hours ago / N days ago，一周以前退回 MM-dd。 */
fun relativeTime(resources: Resources, millis: Long, locale: Locale = English): String {
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
            .format(java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }
}
