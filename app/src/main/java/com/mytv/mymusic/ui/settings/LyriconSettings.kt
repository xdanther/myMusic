package com.mytv.mymusic.ui.settings

import androidx.compose.runtime.Composable
import com.mytv.mymusic.lyricon.LyriconSubscriptionState

@Composable
internal fun LyriconPreviewCard(state: LyriconSubscriptionState) {
    val source = state.playerPackageName.ifBlank { state.providerPackageName }
    val title = when {
        state.timedOut -> "Timed out connecting to Lyricon"
        state.connecting -> "Connecting to Lyricon…"
        !state.connected -> "Waiting for the Lyricon core service"
        source.isBlank() -> "Connected, waiting for an active player"
        else -> "Receiving · $source"
    }
    val body = if (!state.connected || (state.song == null && state.plainText.isNullOrBlank())) {
        if (state.timedOut) "Check that the Lyricon core service is installed and active. A failed connection doesn't affect playback."
        else "Songs and lyrics received from other players appear here."
    } else {
        val song = state.song
        val line = state.currentLine
        buildList {
            song?.name?.takeIf { it.isNotBlank() }?.let(::add)
            song?.artist?.takeIf { it.isNotBlank() }?.let(::add)
            if (!state.plainText.isNullOrBlank()) {
                add(state.plainText)
            } else if (line != null) {
                add(line.text)
                if (state.displayRoma && line.romanization.isNotBlank()) add(line.romanization)
                if (state.displayTranslation && line.translation.isNotBlank()) add(line.translation)
            } else {
                add("Waiting for lyrics")
            }
        }.joinToString("\n")
    }
    TextCard(title = title, body = body, index = 0, count = 1)
}
