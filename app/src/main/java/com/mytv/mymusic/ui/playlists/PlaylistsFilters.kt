package com.mytv.mymusic.ui.playlists

import com.mytv.mymusic.data.PlaylistCategory

/**
 * 「全部」是一个**值**，不是文案：DataStore 里的 `listen_group`、平台返回的 [PlaylistCategory.group]
 * 一直用的都是中文「全部」。翻译只能发生在显示的时候（`R.string.all_items`），
 * 否则旧数据、同步文件和平台分类都会对不上。
 */
internal const val GROUP_ALL = "All"

/** 分类分组 chip：固定“全部”在首位，其余去空、去重并保持服务端顺序。 */
internal fun categoryGroups(categories: List<PlaylistCategory>): List<String> =
    listOf(GROUP_ALL) + categories.map { it.group }.filter { it.isNotBlank() && it != GROUP_ALL }.distinct()

/** 当前分组下可见的分类；[GROUP_ALL] 包含没有分组的分类。 */
internal fun visibleCategories(categories: List<PlaylistCategory>, group: String): List<PlaylistCategory> =
    categories.filter { group == GROUP_ALL || it.group == group || it.group.isBlank() }
