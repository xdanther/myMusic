package com.mytv.mymusic.ui.settings

import com.mytv.mymusic.data.SwipeAction
import com.mytv.mymusic.data.SwipePrefs
import com.mytv.mymusic.data.SwipeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeSettingsSummaryTest {
    @Test
    fun `default summary names the right swipe buttons and the full action`() {
        assertEquals("Swipe right Download, Add to playlist · Pull to end: Save", swipeSummary(SwipePrefs()))
    }

    @Test
    fun `switched off says so`() {
        assertEquals("Off", swipeSummary(SwipePrefs(enabled = false)))
    }

    @Test
    fun `no buttons and nothing at full`() {
        assertEquals("Swipe right No buttons", swipeSummary(SwipePrefs(start = emptyList(), startFull = SwipeAction.None)))
    }

    @Test
    fun `right to left swaps which side is called right`() {
        assertEquals("Swipe right", swipeSideTitle(SwipeSide.Start, rtl = false))
        assertEquals("Swipe left", swipeSideTitle(SwipeSide.End, rtl = false))
        assertEquals("Swipe left", swipeSideTitle(SwipeSide.Start, rtl = true))
        assertEquals("Swipe left Download, Add to playlist · Pull to end: Save", swipeSummary(SwipePrefs(), rtl = true))
    }

    @Test
    fun `tv settings do not list swipe`() {
        assertFalse(SettingsSection.groupsFor(tv = true).flatten().contains(SettingsSection.Swipe))
        assertTrue(SettingsSection.groupsFor(tv = false).flatten().contains(SettingsSection.Swipe))
    }
}
