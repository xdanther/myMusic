package com.mytv.mymusic.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mytv.mymusic.MyMusicApp
import com.mytv.mymusic.data.Song
import com.mytv.mymusic.data.lx.LxPlatform
import com.mytv.mymusic.data.lx.LxSourceRecord
import com.mytv.mymusic.data.lx.LxSources
import com.mytv.mymusic.data.lx.LxUpdateNotice
import com.mytv.mymusic.data.lx.MAX_SCRIPT_BYTES
import com.mytv.mymusic.data.lx.decodeUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal data class LxOperationsUi(
    val busy: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
    val tests: Map<String, String> = emptyMap(),
    val message: String = "",
)

internal class LxSourcesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyMusicApp
    private val mutableOperations = MutableStateFlow(LxOperationsUi())
    private val mutableNotice = MutableStateFlow<LxUpdateNotice?>(null)
    val operations = mutableOperations.asStateFlow()
    val notice = mutableNotice.asStateFlow()
    val records = LxSources.records

    init {
        viewModelScope.launch { LxSources.updateNotices.collect { mutableNotice.value = it.firstOrNull() } }
    }

    fun importOnline(url: String, onSuccess: () -> Unit) = operate("import", "Source added", onSuccess) {
        LxSources.importOnline(url)
    }

    fun importLocal(uri: Uri, replaceId: String? = null, onSuccess: () -> Unit) =
        operate(if (replaceId == null) "import" else "update:$replaceId", if (replaceId == null) "Source added" else "Source updated", onSuccess) {
            val script = readScript(uri)
            LxSources.importLocal(script, replaceId)
        }

    fun update(record: LxSourceRecord) = operate("update:${record.id}", "Source updated") {
        LxSources.update(record)
    }

    fun setEnabled(record: LxSourceRecord, enabled: Boolean) = operate("toggle:${record.id}", "") {
        LxSources.setEnabled(record.id, enabled)
    }

    fun move(record: LxSourceRecord, delta: Int) = operate("move:${record.id}", "") {
        LxSources.move(record.id, delta)
    }

    fun delete(record: LxSourceRecord) = operate("delete:${record.id}", "Deleted ${record.name}") {
        LxSources.delete(record.id)
    }

    fun test(record: LxSourceRecord, current: Song?, cookies: Map<String, String>) =
        operate("test:${record.id}", "") {
            val started = System.currentTimeMillis()
            val song = current?.takeIf { song ->
                LxPlatform.sourceKey(song.source)?.let { record.capabilities[it]?.actions?.contains("musicUrl") } == true
            } ?: findTestSong(record, cookies)
            val result = LxSources.resolveFrom(record, song)
            val media = LxSources.probeStream(result.stream)
            val elapsed = (System.currentTimeMillis() - started) / 1000.0
            val platform = LxPlatform.labels[LxPlatform.sourceKey(song.source)].orEmpty()
            val quality = result.quality.ifBlank { "Default quality" }
            mutableOperations.update {
                it.copy(tests = it.tests + (record.id to "Media read successfully · $platform · $quality · ${"%.1f".format(elapsed)} s\n${song.title}\n$media"))
            }
        }

    fun dismissNotice() {
        mutableNotice.value?.let { LxSources.dismissUpdateNotice(it.sourceId) }
        mutableNotice.value = null
    }
    fun clearMessage() { mutableOperations.update { it.copy(message = "") } }

    private suspend fun findTestSong(record: LxSourceRecord, cookies: Map<String, String>): Song {
        val failures = mutableListOf<String>()
        for (source in record.capabilities.keys) {
            val provider = LxPlatform.providerId(source) ?: continue
            if (record.capabilities[source]?.actions?.contains("musicUrl") != true) continue
            try {
                val song = app.repository.searchSongs(provider, cookies[provider].orEmpty(), "Sunny Day")
                    .firstOrNull { !it.isInvalid && it.id.isNotBlank() }
                if (song != null) return song
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failures += error.message.orEmpty()
            }
        }
        throw IllegalStateException(
            if (failures.isEmpty()) "No test song found; play a song from a platform this source supports first"
            else "Test song search failed; play a song from a platform this source supports first",
        )
    }

    private suspend fun readScript(uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = app.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_SCRIPT_BYTES) throw IllegalArgumentException("The script is over 2 MB and can't be imported")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Couldn't read the selected file")
        decodeUtf8(bytes)
    }

    private fun operate(key: String, successMessage: String, onSuccess: (() -> Unit)? = null, block: suspend () -> Any?) {
        if (key in mutableOperations.value.busy) return
        mutableOperations.update {
            val clearTest = key.takeIf { it.startsWith("test:") }?.substringAfter(':')
            it.copy(
                busy = it.busy + key,
                errors = it.errors - key,
                tests = if (clearTest == null) it.tests else it.tests - clearTest,
                message = "",
            )
        }
        viewModelScope.launch {
            try {
                withTimeoutOrNull(120_000) { block() } ?: throw IllegalStateException("Action timed out; try again later")
                if (key.startsWith("update:")) {
                    mutableOperations.update { it.copy(tests = it.tests - key.substringAfter(':')) }
                }
                if (successMessage.isNotBlank()) mutableOperations.update { it.copy(message = successMessage) }
                onSuccess?.invoke()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message?.lineSequence()?.firstOrNull()?.take(500) ?: "Action failed"
                mutableOperations.update { it.copy(errors = it.errors + (key to message)) }
            } finally {
                mutableOperations.update { it.copy(busy = it.busy - key) }
            }
        }
    }
}
