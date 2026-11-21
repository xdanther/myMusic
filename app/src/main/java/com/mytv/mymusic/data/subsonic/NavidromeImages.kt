package com.mytv.mymusic.data.subsonic

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 封面图的认证。
 *
 * 封面地址（[SubsonicApi.coverUrl]）不含令牌，这样歌单、收藏、同步文件里存下来的
 * 只是一条普通地址；真正发请求时由这里补上 `u / t / s`。只处理当前 Navidrome
 * 那台服务器的请求，别的图片原样放行。
 */
object NavidromeImages {
    /** 交给 Coil 的客户端（见 `MyMusicApp.newImageLoader`）。 */
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val account = NavidromeStore.current()
                val request = chain.request()
                chain.proceed(if (account == null) request else signed(account, request))
            }
            .build()
    }

    /** 是这台 Navidrome 的 `rest/` 地址就补认证，否则原样返回。 */
    fun signed(account: NavidromeAccount, request: Request): Request {
        val base: HttpUrl = account.baseUrl ?: return request
        val url = request.url
        if (url.scheme != base.scheme || url.host != base.host || url.port != base.port) return request
        val prefix = base.pathSegments.filter { it.isNotEmpty() }
        val segments = url.pathSegments.filter { it.isNotEmpty() }
        if (segments.size < prefix.size || segments.subList(0, prefix.size) != prefix) return request
        val builder = url.newBuilder()
        SubsonicAuth.params(account.username, account.password).forEach { (key, value) ->
            if (url.queryParameter(key) == null) builder.addQueryParameter(key, value)
        }
        return request.newBuilder().url(builder.build()).build()
    }
}
