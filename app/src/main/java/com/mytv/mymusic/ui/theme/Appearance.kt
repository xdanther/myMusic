package com.mytv.mymusic.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.mytv.mymusic.R
import kotlin.math.abs

/** 品牌松绿，封面取色失败、没有壁纸取色时的兜底种子色。 */
val BrandSeed = Color(0xFF006B5F)

/** 显示名走资源（[labelRes]）；枚举名才是存进 DataStore 的值，改文案不影响已保存的设置。 */
enum class ColorSource(@StringRes val labelRes: Int, @StringRes val hintRes: Int) {
    Cover(R.string.color_source_cover, R.string.color_source_cover_hint),
    Wallpaper(R.string.color_source_wallpaper, R.string.color_source_wallpaper_hint),
    Brand(R.string.color_source_brand, R.string.color_source_brand_hint),
    Custom(R.string.color_source_custom, R.string.color_source_custom_hint),
}

enum class SchemeStyle(@StringRes val labelRes: Int, val palette: PaletteStyle) {
    TonalSpot(R.string.scheme_soft, PaletteStyle.TonalSpot),
    Vibrant(R.string.scheme_vivid, PaletteStyle.Vibrant),
    Expressive(R.string.scheme_expressive, PaletteStyle.Expressive),
    Fidelity(R.string.scheme_fidelity, PaletteStyle.Fidelity),
}

enum class BlurMode(@StringRes val labelRes: Int) {
    Auto(R.string.blur_auto),
    On(R.string.blur_on),
    Off(R.string.blur_off),
}

/** 布局模式：自动按设备判断，其余同时把屏幕方向锁到对应形态。 */
enum class LayoutMode(@StringRes val labelRes: Int) {
    Auto(R.string.layout_auto),
    Phone(R.string.layout_phone),
    Tablet(R.string.layout_tablet),
    Tv(R.string.layout_tv),
}

/**
 * 界面文字大小：把 [scale] 乘到系统字号上，只放大文字，dp 尺寸不变。
 * 电视三米开外看不清小字时往上调；列表和卡片会随着文字变高。
 */
enum class FontScale(@StringRes val labelRes: Int, val scale: Float) {
    Standard(R.string.font_standard, 1f),
    Large(R.string.font_large, 1.15f),
    Larger(R.string.font_larger, 1.3f),
    Largest(R.string.font_largest, 1.5f),
    ;

    companion object {
        fun of(scale: Float): FontScale = entries.minByOrNull { abs(it.scale - scale) } ?: Standard
    }
}

/** 外观设置的完整快照，主题层只读这一个对象。 */
@Immutable
data class Appearance(
    val theme: String = "system",
    val layout: LayoutMode = LayoutMode.Auto,
    val colorSource: ColorSource = ColorSource.Cover,
    val customSeed: Int = BrandSeed.toArgb(),
    val browseStyle: SchemeStyle = SchemeStyle.TonalSpot,
    val playerStyle: SchemeStyle = SchemeStyle.Vibrant,
    val contrast: Float = 0f,
    val fontScale: FontScale = FontScale.Standard,
    val amoled: Boolean = false,
    val blur: BlurMode = BlurMode.Auto,
    val reduceMotion: Boolean = false,
    val lastSeed: Int = 0,
) {
    fun isDark(system: Boolean): Boolean = when (theme) {
        "dark" -> true
        "light" -> false
        else -> system
    }
}

/** 自选颜色的色板：覆盖色相环，彩度足够让 HCT 生成有区分度的配色。 */
val CustomSeedSwatches = listOf(
    Color(0xFF006B5F), Color(0xFF1F6F78), Color(0xFF3A5BA0), Color(0xFF4B3F9E),
    Color(0xFF7D4BA0), Color(0xFFD9668F), Color(0xFFB8322A), Color(0xFFE8833A),
    Color(0xFFB08B47), Color(0xFF5E7D3A),
)
