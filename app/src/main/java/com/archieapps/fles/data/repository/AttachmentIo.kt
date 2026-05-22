package com.archieapps.fles.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.PendingAttachment
import java.io.File
import java.util.UUID

class AttachmentIo(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { it.mkdirs() }
    private val downloadsDir: File
        get() = File(context.filesDir, "downloads").also { it.mkdirs() }

    fun pendingFromUri(uri: Uri, mimeHint: String? = null): PendingAttachment {
        val info = queryInfo(uri)
        val fileName = info.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "arquivo"
        val mimeType = mimeHint ?: resolver.getType(uri) ?: "application/octet-stream"
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val kind = classify(mimeType, fileName)
        val isImage = mimeType.startsWith("image/") || extension in setOf("png", "jpg", "jpeg", "gif", "webp", "heic", "bmp")
        return PendingAttachment(
            uri = uri,
            fileName = fileName,
            mimeType = mimeType,
            size = info.size ?: 0L,
            kind = kind,
            extension = extension,
            isImage = isImage
        )
    }

    fun readBytes(uri: Uri): ByteArray {
        return resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Não foi possível ler o arquivo")
    }

    fun readBytes(path: String): ByteArray {
        return File(path.removePrefix("file://")).readBytes()
    }

    fun persistAttachment(localId: String, attachment: PendingAttachment): File {
        val extension = attachment.extension.ifBlank { "bin" }.replace(Regex("[^a-zA-Z0-9]"), "").take(8).ifBlank { "bin" }
        val file = File(attachmentsDir, "$localId.$extension")
        file.writeBytes(readBytes(attachment.uri))
        return file
    }

    fun persistAttachmentBytes(localId: String, extensionValue: String, bytes: ByteArray): File {
        val extension = extensionValue.ifBlank { "bin" }.replace(Regex("[^a-zA-Z0-9]"), "").take(8).ifBlank { "bin" }
        val file = File(attachmentsDir, "$localId.$extension")
        file.writeBytes(bytes)
        return file
    }

    fun persistDownloadedFile(localId: String, fileName: String, bytes: ByteArray): File {
        val safe = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "arquivo" }
        val file = File(downloadsDir, "$localId-$safe")
        file.writeBytes(bytes)
        return file
    }

    fun writeCacheFile(fileName: String, bytes: ByteArray): File {
        val safe = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "arquivo" }
        val file = File(context.cacheDir, "${UUID.randomUUID()}-$safe")
        file.writeBytes(bytes)
        return file
    }

    fun clearTemporaryCache() {
        context.cacheDir.listFiles()?.forEach { file -> runCatching { file.deleteRecursively() } }
    }

    private fun queryInfo(uri: Uri): FileInfo {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use FileInfo()
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                FileInfo(
                    name = if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                )
            } ?: FileInfo()
        }.getOrDefault(FileInfo())
    }

    private fun classify(mimeType: String, fileName: String): FileKind {
        val mime = mimeType.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            mime.startsWith("image/") -> FileKind.Image
            mime.startsWith("video/") -> FileKind.Video
            mime.startsWith("audio/") -> FileKind.Audio
            mime == "application/pdf" || ext == "pdf" -> FileKind.Pdf
            ext in setOf("zip", "rar", "7z", "tar", "gz") -> FileKind.Archive
            ext in setOf("xls", "xlsx", "csv", "ods") -> FileKind.Spreadsheet
            ext in setOf("ppt", "pptx", "odp") -> FileKind.Presentation
            ext in setOf("js", "ts", "kt", "java", "py", "php", "html", "css", "json", "md", "xml", "sql") -> FileKind.Code
            mime.startsWith("text/") || ext in setOf("txt", "rtf") -> FileKind.Text
            mime.contains("document") || ext in setOf("doc", "docx", "odt") -> FileKind.Document
            else -> FileKind.Other
        }
    }
}

private data class FileInfo(
    val name: String? = null,
    val size: Long? = null
)
