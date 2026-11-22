package com.mytv.mymusic.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Precondition：带条件的写入被拒（WebDAV 412），说明读完到写入之间云端被别的设备改了。 */
enum class SyncFailureKind { Auth, Permission, NotFound, Network, Corrupt, Precondition, Unknown }

class SyncException(val kind: SyncFailureKind, override val message: String) : Exception(message)

data class RemoteAccount(val id: String, val login: String)

data class RemoteFile(
    val id: String,
    val content: String,
    val revision: String,
    val updatedAt: Long,
    val htmlUrl: String,
)

interface PlaylistSyncProvider {
    val id: String
    val label: String
    val fileName: String

    suspend fun validate(token: String): RemoteAccount

    /**
     * 同步文件位置固定的后端（WebDAV：`<目录>/mymusic-library.json`）用它找已有文件，找到返回
     * remoteId，没有返回空串。Gist / Gitee 的文件 ID 只能由用户给，默认就是空串。
     */
    suspend fun locate(token: String): String = ""

    suspend fun read(token: String, remoteId: String): RemoteFile
    suspend fun create(token: String, content: String): RemoteFile

    /**
     * [expectedRevision] 是写入前刚读到的 [RemoteFile.revision]。支持条件写入的后端（WebDAV 的
     * If-Match）在云端已经变了时抛 [SyncFailureKind.Precondition]；Gist / Gitee 没有条件写入，
     * 忽略它，靠同步流程里“上传前再读一次”比较。
     */
    suspend fun update(token: String, remoteId: String, content: String, expectedRevision: String = ""): RemoteFile
}

internal fun syncHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(40, TimeUnit.SECONDS)
    .writeTimeout(40, TimeUnit.SECONDS)
    .build()

private val jsonMedia = "application/json; charset=utf-8".toMediaType()
private val json = Json { ignoreUnknownKeys = true }

internal fun extractRemoteId(input: String): String {
    val value = input.trim().trimEnd('/')
    if (value.isBlank()) return ""
    val noQuery = value.substringBefore('?').substringBefore('#')
    return noQuery.substringAfterLast('/').ifBlank { noQuery }
}

private fun parseIso(value: String?): Long =
    runCatching { Instant.parse(value.orEmpty()).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

private fun Request.Builder.bearer(token: String) = apply {
    header("Authorization", "Bearer $token")
    header("Accept", "application/vnd.github+json")
    header("X-GitHub-Api-Version", "2022-11-28")
    header("User-Agent", "myMusic-Android")
}

class GistSyncProvider(private val http: OkHttpClient = syncHttpClient()) : PlaylistSyncProvider {
    override val id = "gist"
    override val label = "GitHub Gist"
    override val fileName = "mymusic-library.json"
    private val api = "https://api.github.com".toHttpUrl()

    override suspend fun validate(token: String): RemoteAccount = call {
        val request = Request.Builder().url(api.newBuilder().addPathSegment("user").build()).bearer(token).build()
        val body = execute(request)
        RemoteAccount(
            id = body["id"]?.jsonPrimitive?.content.orEmpty(),
            login = body["login"]?.jsonPrimitive?.content.orEmpty(),
        ).also {
            if (it.login.isBlank()) throw SyncException(SyncFailureKind.Auth, "The token can't read the GitHub account")
        }
    }

    override suspend fun read(token: String, remoteId: String): RemoteFile = call {
        val request = Request.Builder().url(gistUrl(remoteId)).bearer(token).build()
        parseFile(execute(request))
    }

    override suspend fun create(token: String, content: String): RemoteFile = call {
        val payload = buildJsonObject {
            put("description", "myMusic playlist sync")
            put("public", false)
            put(
                "files",
                buildJsonObject { put(fileName, buildJsonObject { put("content", content) }) },
            )
        }
        val request = Request.Builder()
            .url(api.newBuilder().addPathSegment("gists").build())
            .bearer(token)
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()
        parseFile(execute(request))
    }

    override suspend fun update(token: String, remoteId: String, content: String, expectedRevision: String): RemoteFile = call {
        val payload = buildJsonObject {
            put("files", buildJsonObject { put(fileName, buildJsonObject { put("content", content) }) })
        }
        val request = Request.Builder()
            .url(gistUrl(remoteId))
            .bearer(token)
            .patch(payload.toString().toRequestBody(jsonMedia))
            .build()
        parseFile(execute(request))
    }

    private fun gistUrl(remoteId: String) = api.newBuilder().addPathSegment("gists").addPathSegment(remoteId).build()

    private fun parseFile(root: JsonObject): RemoteFile {
        val file = root["files"]?.jsonObject?.get(fileName)?.jsonObject
            ?: throw SyncException(SyncFailureKind.Corrupt, "The cloud Gist has no $fileName, so syncing can't continue.")
        val content = file["content"]?.jsonPrimitive?.content
            ?: throw SyncException(SyncFailureKind.Corrupt, "The cloud file couldn't be read.")
        val revision = runCatching {
            root["history"]?.jsonArray?.firstOrNull()?.jsonObject?.get("version")?.jsonPrimitive?.content
        }.getOrNull().orEmpty()
            .ifBlank { root["updated_at"]?.jsonPrimitive?.content.orEmpty() }
        return RemoteFile(
            id = root["id"]?.jsonPrimitive?.content.orEmpty(),
            content = content,
            revision = revision,
            updatedAt = parseIso(root["updated_at"]?.jsonPrimitive?.content),
            htmlUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty(),
        )
    }

    private suspend fun execute(request: Request): JsonObject = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body.string()
                if (!response.isSuccessful) throw httpFailure(response.code, raw)
                runCatching { Json.parseToJsonElement(raw).jsonObject }
                    .getOrElse { JsonObject(emptyMap()) }
            }
        } catch (error: SyncException) {
            throw error
        } catch (error: IOException) {
            throw SyncException(SyncFailureKind.Network, "Network unreachable: ${error.message ?: "check the connection"}")
        }
    }
}

class GiteeSyncProvider(private val http: OkHttpClient = syncHttpClient()) : PlaylistSyncProvider {
    override val id = "gitee"
    override val label = "Gitee snippets"
    override val fileName = "mymusic-library.json"
    private val api = "https://gitee.com/api/v5".toHttpUrl()

    override suspend fun validate(token: String): RemoteAccount = call {
        val url = api.newBuilder().addPathSegment("user").addQueryParameter("access_token", token).build()
        val root = execute(Request.Builder().url(url).get().header("User-Agent", "myMusic-Android").build())
        RemoteAccount(
            id = root["id"]?.jsonPrimitive?.content.orEmpty(),
            login = root["login"]?.jsonPrimitive?.content.orEmpty(),
        ).also {
            if (it.login.isBlank()) throw SyncException(SyncFailureKind.Auth, "The token can't read the Gitee account")
        }
    }

    override suspend fun read(token: String, remoteId: String): RemoteFile = call {
        val url = api.newBuilder()
            .addPathSegment("gists").addPathSegment(remoteId)
            .addQueryParameter("access_token", token)
            .build()
        parseFile(execute(Request.Builder().url(url).get().header("User-Agent", "myMusic-Android").build()))
    }

    override suspend fun create(token: String, content: String): RemoteFile = call {
        val payload = buildJsonObject {
            put("access_token", token)
            put("description", "myMusic playlist sync")
            put("public", false)
            put(
                "files",
                buildJsonObject {
                    put(fileName, buildJsonObject {
                        put("content", content)
                        put("type", "application/json")
                    })
                },
            )
        }
        val url = api.newBuilder().addPathSegment("gists").build()
        parseFile(
            execute(
                Request.Builder().url(url)
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .header("User-Agent", "myMusic-Android")
                    .build()
            )
        )
    }

    override suspend fun update(token: String, remoteId: String, content: String, expectedRevision: String): RemoteFile = call {
        val payload = buildJsonObject {
            put("access_token", token)
            put("description", "myMusic playlist sync")
            put(
                "files",
                buildJsonObject {
                    put(fileName, buildJsonObject {
                        put("content", content)
                        put("type", "application/json")
                    })
                },
            )
        }
        val url = api.newBuilder().addPathSegment("gists").addPathSegment(remoteId).build()
        parseFile(
            execute(
                Request.Builder().url(url)
                    .patch(payload.toString().toRequestBody(jsonMedia))
                    .header("User-Agent", "myMusic-Android")
                    .build()
            )
        )
    }

    private fun parseFile(root: JsonObject): RemoteFile {
        val file = root["files"]?.jsonObject?.get(fileName)?.jsonObject
            ?: throw SyncException(SyncFailureKind.Corrupt, "The cloud snippet has no $fileName, so syncing can't continue.")
        val content = file["content"]?.jsonPrimitive?.content
            ?: throw SyncException(SyncFailureKind.Corrupt, "The cloud file couldn't be read.")
        return RemoteFile(
            id = root["id"]?.jsonPrimitive?.content.orEmpty(),
            content = content,
            revision = root["updated_at"]?.jsonPrimitive?.content ?: "",
            updatedAt = parseIso(root["updated_at"]?.jsonPrimitive?.content),
            htmlUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty(),
        )
    }

    private suspend fun execute(request: Request): JsonObject = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body.string()
                if (!response.isSuccessful) throw httpFailure(response.code, raw)
                runCatching { Json.parseToJsonElement(raw).jsonObject }
                    .getOrElse { JsonObject(emptyMap()) }
            }
        } catch (error: SyncException) {
            throw error
        } catch (error: IOException) {
            throw SyncException(SyncFailureKind.Network, "Network unreachable: ${error.message ?: "check the connection"}")
        }
    }
}

private fun httpFailure(code: Int, raw: String): SyncException {
    val detail = runCatching {
        val root = Json.parseToJsonElement(raw).jsonObject
        root["message"]?.jsonPrimitive?.content.orEmpty()
    }.getOrDefault("")
    return when (code) {
        401 -> SyncException(SyncFailureKind.Auth, "The token is invalid or expired; generate a new one.")
        403 -> SyncException(SyncFailureKind.Permission, "The token lacks permission; grant at least snippet / Gist access.")
        404 -> SyncException(SyncFailureKind.NotFound, "Cloud file not found; check the link or reconnect.")
        else -> SyncException(
            SyncFailureKind.Unknown,
            if (detail.isBlank()) "The sync service returned an error (HTTP $code)" else "The sync service returned an error: $detail",
        )
    }
}

private inline fun <T> call(block: () -> T): T = try {
    block()
} catch (error: SyncException) {
    throw error
} catch (error: Exception) {
    throw SyncException(
        SyncFailureKind.Unknown,
        error.message?.takeIf { it.isNotBlank() } ?: "Sync failed (${error.javaClass.simpleName})",
    )
}
