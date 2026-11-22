package com.mytv.mymusic.ui.settings

import android.content.res.Resources
import androidx.annotation.StringRes
import com.mytv.mymusic.R
import com.mytv.mymusic.data.CoverShapeChoice
import com.mytv.mymusic.data.LyricsBackgroundMode
import com.mytv.mymusic.data.SwipeAction

/**
 * 设置首页的显示名。
 *
 * 只做'显示时映射'：枚举名、平台 id 和存进 DataStore 的值都不动，改文案不会让已保存的设置失效。
 */
internal fun Resources.coverShapeLabel(shape: CoverShapeChoice): String = getString(
    when (shape) {
        CoverShapeChoice.Random -> R.string.cover_shape_random
        CoverShapeChoice.Cookie12 -> R.string.cover_shape_cookie12
        CoverShapeChoice.Cookie9 -> R.string.cover_shape_cookie9
        CoverShapeChoice.Cookie7 -> R.string.cover_shape_cookie7
        CoverShapeChoice.Sunny -> R.string.cover_shape_sunny
        CoverShapeChoice.SoftBurst -> R.string.cover_shape_softburst
        CoverShapeChoice.Clover8 -> R.string.cover_shape_clover8
    },
)

internal fun Resources.lyricsBackgroundLabel(mode: LyricsBackgroundMode): String = getString(
    when (mode) {
        LyricsBackgroundMode.Off -> R.string.lyrics_bg_off
        LyricsBackgroundMode.Ambient -> R.string.lyrics_bg_ambient
        LyricsBackgroundMode.Reactive -> R.string.lyrics_bg_reactive
    },
)

/** 均衡器预设按 id 认（`EqPreset.label` 还是原来的中文，留给均衡器页自己迁移）。 */
@StringRes
internal fun eqPresetName(id: String): Int = when (id) {
    "flat" -> R.string.eq_flat
    "pop" -> R.string.eq_pop
    "rock" -> R.string.eq_rock
    "jazz" -> R.string.eq_jazz
    "vocal" -> R.string.eq_vocal
    else -> R.string.eq_custom
}

internal fun Resources.swipeActionLabel(action: SwipeAction): String = getString(
    when (action) {
        SwipeAction.Download -> R.string.swipe_action_download
        SwipeAction.AddToPlaylist -> R.string.swipe_action_playlist
        SwipeAction.Favorite -> R.string.swipe_action_favorite
        SwipeAction.Unfavorite -> R.string.swipe_action_unfavorite
        SwipeAction.Delete -> R.string.swipe_action_delete
        SwipeAction.PlayNext -> R.string.swipe_action_play_next
        SwipeAction.AddToQueue -> R.string.swipe_action_queue
        SwipeAction.OpenArtist -> R.string.swipe_action_artist
        SwipeAction.OpenAlbum -> R.string.swipe_action_album
        SwipeAction.ChooseSource -> R.string.swipe_action_source
        SwipeAction.None -> R.string.swipe_action_none
    },
)

/**
 * 平台品牌名。接口返回的 `ProviderInfo.name` 是平台自己的中文名（也当数据用），
 * 这里按 id 换成当前语言的显示名；不认识的 id 用原来的名字，绝不改存起来的值。
 */
internal fun Resources.providerBrand(id: String, fallback: String): String {
    val name = when (id) {
        "netease" -> R.string.provider_netease
        "qq" -> R.string.provider_qq
        "kugou" -> R.string.provider_kugou
        "kuwo" -> R.string.provider_kuwo
        "migu" -> R.string.provider_migu
        "apple" -> R.string.provider_apple
        "soda" -> R.string.provider_soda
        "bilibili" -> R.string.provider_bilibili
        "joox" -> R.string.provider_joox
        "fivesing" -> R.string.provider_fivesing
        "qianqian" -> R.string.provider_qianqian
        "local" -> R.string.provider_local
        "lx" -> R.string.provider_lx
        "youtube" -> R.string.provider_youtube
        "jamendo" -> R.string.provider_jamendo
        "mhub" -> R.string.provider_mhub
        else -> null
    }
    return name?.let(::getString) ?: fallback
}
