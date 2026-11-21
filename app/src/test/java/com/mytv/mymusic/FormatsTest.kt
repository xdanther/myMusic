package com.mytv.mymusic

import com.mytv.mymusic.ui.text.formatByteSize
import com.mytv.mymusic.ui.text.formatClock
import com.mytv.mymusic.ui.text.formatCompactCount
import com.mytv.mymusic.ui.text.formatDurationSeconds
import com.mytv.mymusic.ui.text.formatMonthDay
import com.mytv.mymusic.ui.text.monthDayPattern
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** 共用格式化：同一套写法在中文和英文下都要成立，不能退回「万 / 亿 / M 月 d 日」。 */
class FormatsTest {
    @Test
    fun `compact counts use k m b for every language`() {
        assertEquals("999", formatCompactCount(999, Locale.US))
        assertEquals("1.5K", formatCompactCount(1_500, Locale.US))
        assertEquals("12.3K", formatCompactCount(12_345, Locale.US))
        assertEquals("4.5M", formatCompactCount(4_500_000, Locale.US))
        assertEquals("1.2B", formatCompactCount(1_200_000_000, Locale.US))
        assertEquals("", formatCompactCount(0, Locale.US))
        assertEquals("1.5K", formatCompactCount(1_500, Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `month day follows the interface language`() {
        assertEquals("M 月 d 日", monthDayPattern(Locale.SIMPLIFIED_CHINESE))
        assertEquals("MMM d", monthDayPattern(Locale.US))
        assertEquals("10 月 20 日", formatMonthDay(LocalDate.of(2026, 10, 20), Locale.SIMPLIFIED_CHINESE))
        assertEquals("Oct 20", formatMonthDay(LocalDate.of(2026, 10, 20), Locale.US))
    }

    @Test
    fun `clock and byte sizes keep their existing shapes`() {
        assertEquals("03:05", formatClock(185_000, Locale.US))
        assertEquals("00:00", formatClock(-5_000, Locale.US))
        assertEquals("1.5 MB", formatByteSize(1024L * 1024 * 3 / 2, Locale.US))
        assertEquals("512 B", formatByteSize(512, Locale.US))
        assertEquals("2.0 GB", formatByteSize(2L * 1024 * 1024 * 1024, Locale.US))
    }

    @Test
    fun `unknown duration stays blank`() {
        assertEquals("", formatDurationSeconds(0))
        assertEquals("", formatDurationSeconds(-3))
        assertEquals("01:05", formatDurationSeconds(65, Locale.US))
    }
}
