package com.mytv.mymusic

import com.mytv.mymusic.ui.text.formatByteSize
import com.mytv.mymusic.ui.text.formatClock
import com.mytv.mymusic.ui.text.formatCompactCount
import com.mytv.mymusic.ui.text.formatDurationSeconds
import com.mytv.mymusic.ui.text.formatMonthDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 共用格式化：整套界面只有英文，设备是中文时也不能再出现「10 月 20 日」这类写法。 */
class FormatsTest {
    @Test
    fun `compact counts use k m b`() {
        assertEquals("999", formatCompactCount(999))
        assertEquals("1.5K", formatCompactCount(1_500))
        assertEquals("12.3K", formatCompactCount(12_345))
        assertEquals("4.5M", formatCompactCount(4_500_000))
        assertEquals("1.2B", formatCompactCount(1_200_000_000))
        assertEquals("", formatCompactCount(0))
    }

    @Test
    fun `month day stays english`() {
        assertEquals("Oct 20", formatMonthDay(LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `clock and byte sizes keep their existing shapes`() {
        assertEquals("03:05", formatClock(185_000))
        assertEquals("00:00", formatClock(-5_000))
        assertEquals("1.5 MB", formatByteSize(1024L * 1024 * 3 / 2))
        assertEquals("512 B", formatByteSize(512))
        assertEquals("2.0 GB", formatByteSize(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun `unknown duration stays blank`() {
        assertEquals("", formatDurationSeconds(0))
        assertEquals("", formatDurationSeconds(-3))
        assertEquals("01:05", formatDurationSeconds(65))
    }
}
