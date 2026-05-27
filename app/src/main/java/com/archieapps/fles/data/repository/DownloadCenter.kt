package com.archieapps.fles.data.repository

import com.archieapps.fles.ui.model.FileItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class DownloadStatus { Downloading, Completed, Failed }

data class DownloadItem(
    val file: FileItem,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.Downloading,
    val error: String? = null,
    val resultFile: File? = null
) {
    val attachmentId: String get() = file.id

    /** Fração 0..1, ou null quando o tamanho total é desconhecido (progresso indeterminado). */
    val fraction: Float?
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else null
}

/**
 * Fonte única de verdade dos downloads sob demanda. Roda cada download no [scope]
 * do app (não no escopo da tela), deduplicado por id do anexo — assim fechar o
 * visualizador não cancela o download. A UI observa [downloads] para a faixa e os
 * indicadores por item.
 */
class DownloadCenter(
    private val scope: CoroutineScope,
    private val filesRepository: FilesRepository
) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val jobs = HashMap<String, Deferred<File>>()
    private val lock = Any()

    /** Retorna o arquivo decifrado; já-baixados/cacheados voltam na hora (sem faixa). */
    suspend fun ensure(file: FileItem): File {
        val cached = if (file.isSecure) file.previewLocalPath else file.localPath
        cached?.removePrefix("file://")?.let { path ->
            val local = File(path)
            if (local.exists()) return local
        }
        return enqueue(file).await()
    }

    fun enqueue(file: FileItem): Deferred<File> {
        synchronized(lock) {
            jobs[file.id]?.let { if (it.isActive) return it }
            val deferred = scope.async {
                try {
                    put(DownloadItem(file = file, status = DownloadStatus.Downloading))
                    val result = filesRepository.downloadToCache(file) { downloaded, total ->
                        update(file.id) {
                            it.copy(
                                downloadedBytes = downloaded,
                                totalBytes = if (total > 0L) total else it.totalBytes
                            )
                        }
                    }
                    update(file.id) {
                        it.copy(
                            status = DownloadStatus.Completed,
                            resultFile = result,
                            downloadedBytes = if (it.totalBytes > 0L) it.totalBytes else it.downloadedBytes
                        )
                    }
                    autoRemoveCompleted(file.id)
                    result
                } catch (e: CancellationException) {
                    remove(file.id)
                    throw e
                } catch (e: Throwable) {
                    update(file.id) { it.copy(status = DownloadStatus.Failed, error = e.message ?: "Falha ao baixar.") }
                    throw e
                } finally {
                    synchronized(lock) { jobs.remove(file.id) }
                }
            }
            jobs[file.id] = deferred
            return deferred
        }
    }

    fun retry(attachmentId: String) {
        val item = _downloads.value.firstOrNull { it.attachmentId == attachmentId } ?: return
        enqueue(item.file)
    }

    fun dismiss(attachmentId: String) {
        synchronized(lock) { jobs.remove(attachmentId)?.cancel() }
        remove(attachmentId)
    }

    private fun put(item: DownloadItem) {
        _downloads.update { list -> listOf(item) + list.filterNot { it.attachmentId == item.attachmentId } }
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.update { list -> list.map { if (it.attachmentId == id) transform(it) else it } }
    }

    private fun remove(id: String) {
        _downloads.update { list -> list.filterNot { it.attachmentId == id } }
    }

    private fun autoRemoveCompleted(id: String) {
        scope.launch {
            delay(4_000)
            _downloads.update { list -> list.filterNot { it.attachmentId == id && it.status == DownloadStatus.Completed } }
        }
    }
}
