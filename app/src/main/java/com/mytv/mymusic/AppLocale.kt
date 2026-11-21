package com.mytv.mymusic

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

/**
 * 应用语言。除了 [System] 以外的取值会在应用内覆盖系统语言。
 * [tag] 是 BCP 47 语言标签，同时用于 [android.content.res.Resources] 的目录匹配（values-zh 等）。
 */
enum class AppLanguage(val tag: String?, @StringRes val labelRes: Int) {
    System(null, R.string.language_system),
    English("en", R.string.language_english),
    Chinese("zh", R.string.language_chinese),
    ;

    companion object {
        fun ofTag(raw: String?): AppLanguage = entries.firstOrNull { it.tag == raw } ?: System
    }
}

/**
 * 语言偏好：System / 中文 / English。
 *
 * - Android 13+ 用系统的 per-app locale（[LocaleManager]），系统设置里的「应用语言」也能改，改完系统自己重建界面；
 * - 更低版本没有这个 API，把语言写进 Context 的配置，改完重建当前 Activity。
 *
 * 注意：API 33+ 的 per-app locale **不会**改 [Locale.getDefault]，需要跟随界面语言的格式化请用
 * `currentLocale()`（Compose）或 `resources.configuration.locales[0]`。
 */
object AppLocale {
    private const val PREFS_NAME = "app_language"
    private const val KEY_TAG = "tag"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 当前生效的语言。API 33+ 以系统记录为准（用户可能在系统设置里改过），低版本读本机偏好。 */
    fun current(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return AppLanguage.ofTag(locales.toLanguageTags().substringBefore(','))
        }
        return AppLanguage.ofTag(prefs(context).getString(KEY_TAG, null))
    }

    /** 要覆盖的语言；跟随系统时是 null。 */
    fun locale(context: Context): Locale? = current(context).tag?.let { Locale.forLanguageTag(it) }

    fun set(context: Context, language: AppLanguage) {
        // 低版本要同步读，所以镜像一份到 SharedPreferences；API 33+ 只作为缓存
        prefs(context).edit().putString(KEY_TAG, language.tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
            return
        }
        Locale.setDefault(locale(context) ?: systemLocale())
        // 低版本只能自己重建：资源已经按旧语言解析过了
        ForegroundActivity.get()?.recreate()
    }

    /**
     * 在 Application / Activity 的 `attachBaseContext` 里包一层，低版本让资源按选中的语言解析。
     * API 33+ 由系统处理，跟随系统时也不需要包装，原样返回。
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val language = AppLanguage.ofTag(prefs(base).getString(KEY_TAG, null))
        val locale = language.tag?.let { Locale.forLanguageTag(it) } ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return base.createConfigurationContext(config)
    }

    /** 系统自己的语言：改回「跟随系统」时用它覆盖之前写进默认 Locale 的值。 */
    private fun systemLocale(): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale
        }
}
