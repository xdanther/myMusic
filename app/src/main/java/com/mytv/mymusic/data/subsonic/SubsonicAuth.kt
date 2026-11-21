package com.mytv.mymusic.data.subsonic

import java.security.MessageDigest
import java.util.UUID

/**
 * Subsonic 的请求签名：`t = md5(密码 + 盐)`，盐每个请求现生成，密码本身从不出现在地址里。
 * API 请求和封面图片请求共用这一份，免得两处算法写出差别。
 */
internal object SubsonicAuth {
    fun params(username: String, password: String): List<Pair<String, String>> {
        val salt = UUID.randomUUID().toString().replace("-", "").take(12)
        return listOf(
            "u" to username,
            "t" to md5(password + salt),
            "s" to salt,
            "v" to SUBSONIC_API_VERSION,
            "c" to SUBSONIC_CLIENT_NAME,
        )
    }

    fun md5(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}
