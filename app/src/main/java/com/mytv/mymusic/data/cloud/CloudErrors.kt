package com.mytv.mymusic.data.cloud

import java.io.IOException
import java.util.Locale

/**
 * 云端失败的种类。[retryable] 决定界面给不给“重试”：登录、风控、容量这几类重试也没用，
 * 得先让用户处理（规格 12、16.5）。
 */
enum class CloudErrorKind(val retryable: Boolean) {
    /** 没登录或 Cookie 失效（网易 301、QQ 1000）。上传队列遇到它就整体暂停。 */
    NotLoggedIn(false),

    /** 网易 -460 / -462：网络环境有风险。暂停整个队列，等用户点“继续”，不自动重试。 */
    RiskControl(false),

    /** 云盘空间不够；[CloudException.shortfall] 是还差的字节数。 */
    QuotaExceeded(false),

    /** 单个文件超过 500 MB（网易）或服务器限制了单次上传大小（WebDAV 413）。 */
    FileTooLarge(false),

    /** 空文件之类根本没法上传的。 */
    InvalidFile(false),

    /** 转码 10 分钟还没完成。 */
    TranscodeTimeout(true),

    /** 云盘处理文件时报了异常状态。 */
    TranscodeFailed(true),

    /** QQ 音乐云盘只读。 */
    UploadUnsupported(false),

    /** 这个存储不提供这项能力（比如 WebDAV 没有歌词）。 */
    Unsupported(false),

    /** 云端有这个文件但取不到播放地址（QQ 原文件、网易云盘地址接口都失败）。 */
    Unplayable(false),

    /** 平台和云盘都没有这首的歌词。 */
    NoLyrics(false),

    /** The cloud file was deleted（404）；调用方把 remote.status 置成 missing，不删歌单条目。 */
    NotFound(false),

    /** WebDAV 401：密码不对或已失效。 */
    Unauthorized(false),

    /** WebDAV 403 等权限问题。 */
    Forbidden(false),

    /** WebDAV 同一位置已有大小不同的文件。 */
    Conflict(false),

    /** The cloud can't be reached right now（断网、超时）。不写进同步文件，网络恢复自动好。 */
    Network(true),

    /** 平台返回了错误码或 HTTP 错误。 */
    Server(true),

    /** 返回的内容看不懂。 */
    InvalidResponse(true),

    Cancelled(true),
}

/**
 * 云端操作的失败。继承 IOException：接到 Media3 / 下载时能沿着 IO 异常的路径走。
 * [message] 是给用户看的中文，不夹服务器原文里的内部信息。
 */
class CloudException(
    val kind: CloudErrorKind,
    message: String,
    val provider: CloudProvider? = null,
    /** 平台错误码或 HTTP 状态码；没有为 0。 */
    val code: Int = 0,
    /** [CloudErrorKind.QuotaExceeded] 时还差多少字节。 */
    val shortfall: Long = 0,
    cause: Throwable? = null,
) : IOException(message, cause) {
    val retryable: Boolean get() = kind.retryable

    override val message: String get() = super.message.orEmpty()

    /** 遇到这些就该暂停整个上传队列：后面的文件只会一样失败（规格 12：-460 / 401 不要循环重试）。 */
    val pausesQueue: Boolean
        get() = kind == CloudErrorKind.RiskControl || kind == CloudErrorKind.NotLoggedIn ||
            kind == CloudErrorKind.Unauthorized
}

/** 设计稿“出错时用户看到什么”那张表的文案，集中在这里。 */
object CloudMessages {
    const val RISK_CONTROL = "NetEase flagged the network as risky; uploads are paused"

    /** 我的页那一行的说明（设计稿第 09 节）。 */
    const val RISK_CONTROL_PAUSED = "Upload paused: NetEase requires verification again"
    const val QQ_UPLOAD_UNSUPPORTED = "The QQ Music cloud drive is read only; uploads aren't supported yet"
    const val QQ_ORIGINAL_UNPLAYABLE = "The original file in the QQ cloud drive can't play"
    const val NETEASE_UNPLAYABLE = "No playback URL for this NetEase cloud track right now"
    const val TRANSCODE_TIMEOUT = "NetEase transcoding took over 10 minutes; try again later"
    const val CLOUD_UNREACHABLE = "The cloud can't be reached right now"
    const val CLOUD_DELETED = "The cloud file was deleted"
    const val WEBDAV_UNAUTHORIZED = "The WebDAV password is wrong or has expired"
    const val WEBDAV_NO_RANGE = "This server requires a full download before playback"
    const val CANCELLED = "Cancelled"
    const val EMPTY_FILE = "The file is empty and can't be uploaded"
    const val NO_LYRICS = "No lyrics found for this track"

    fun notLoggedIn(provider: CloudProvider): String = when (provider) {
        CloudProvider.NeteaseCloud -> "Not signed in to NetEase Cloud Music, or the session expired"
        CloudProvider.QqCloud -> "Not signed in to QQ Music, or the session expired"
        CloudProvider.WebDav -> "WebDAV isn't configured"
    }

    fun quotaExceeded(shortfall: Long): String = "Not enough cloud space; ${formatSize(shortfall)} short"

    fun fileTooLarge(limit: Long): String = "Files over ${formatSize(limit)} aren't accepted and were skipped"

    fun transcodeFailed(status: Int): String = "The NetEase cloud drive failed on this file (status $status); you can retry"

    fun server(provider: CloudProvider, code: Int): String = "${provider.label} returned error $code"

    /** `1.2 GB` / `412 MB` / `36 KB`，按 1024 进位；GB 保留一位小数，整数时不写 `.0`。 */
    fun formatSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.ROOT, "%.1f", bytes / gb).removeSuffix(".0") + " GB"
            bytes >= mb -> String.format(Locale.ROOT, "%.0f", bytes / mb) + " MB"
            bytes >= kb -> String.format(Locale.ROOT, "%.0f", bytes / kb) + " KB"
            else -> "$bytes B"
        }
    }
}
