package com.archieapps.fles.ui.model

import androidx.compose.ui.graphics.Color

enum class MessageType {
    Text,
    List,
    Image,
    Document,
    Mixed
}

enum class SyncStatus {
    Synced,
    PendingCreate,
    PendingUpload,
    Error
}

enum class FileKind {
    Image,
    Video,
    Audio,
    Pdf,
    Archive,
    Spreadsheet,
    Presentation,
    Document,
    Code,
    Text,
    Link,
    Other
}

data class MockChannel(
    val id: String,
    val name: String,
    val lastMessage: String,
    val updatedAt: String,
    val icon: String,
    val color: Color,
    val syncStatus: SyncStatus = SyncStatus.Synced
)

data class MockListItem(
    val id: String,
    val text: String,
    val checked: Boolean
)

data class MockAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val kind: FileKind,
    val createdAt: String,
    val previewColor: Color? = null
)

data class MockMessage(
    val id: String,
    val folderId: String,
    val type: MessageType,
    val content: String,
    val createdAt: String,
    val isFavorite: Boolean,
    val syncStatus: SyncStatus,
    val attachments: List<MockAttachment> = emptyList(),
    val listItems: List<MockListItem> = emptyList()
)

data class MockFile(
    val id: String,
    val name: String,
    val kind: FileKind,
    val size: Long,
    val folderId: String,
    val folderName: String,
    val messageId: String,
    val isFavorite: Boolean,
    val createdAt: String,
    val previewColor: Color? = null
)

object MockData {
    val channels = listOf(
        MockChannel(
            id = "inbox",
            name = "Inbox",
            lastMessage = "Links e arquivos recebidos",
            updatedAt = "2026-05-20T09:15:00",
            icon = "Inbox",
            color = Color(0xFF3390EC)
        ),
        MockChannel(
            id = "work",
            name = "Trabalho",
            lastMessage = "Briefing, contrato e imagens do projeto",
            updatedAt = "2026-05-20T08:45:00",
            icon = "Briefcase",
            color = Color(0xFF8E85EE)
        ),
        MockChannel(
            id = "study",
            name = "Estudos",
            lastMessage = "Checklist de leitura da semana",
            updatedAt = "2026-05-19T20:30:00",
            icon = "GraduationCap",
            color = Color(0xFF4DD0A8)
        ),
        MockChannel(
            id = "images",
            name = "Imagens",
            lastMessage = "Mockups exportados em PNG",
            updatedAt = "2026-05-18T18:20:00",
            icon = "Image",
            color = Color(0xFFE8618C)
        )
    )

    val messages = listOf(
        MockMessage(
            id = "m1",
            folderId = "inbox",
            type = MessageType.Text,
            content = "Guardar esse link para revisar depois: https://example.com/product-roadmap",
            createdAt = "2026-05-20T09:15:00",
            isFavorite = true,
            syncStatus = SyncStatus.Synced,
            attachments = listOf(
                MockAttachment("a1", "product-roadmap.url", "text/uri-list", 2048, FileKind.Link, "2026-05-20T09:15:00")
            )
        ),
        MockMessage(
            id = "m2",
            folderId = "work",
            type = MessageType.Mixed,
            content = "Arquivos do cliente para a reunião de amanhã.",
            createdAt = "2026-05-20T08:45:00",
            isFavorite = false,
            syncStatus = SyncStatus.PendingUpload,
            attachments = listOf(
                MockAttachment("a2", "contrato-revisado.pdf", "application/pdf", 1_840_000, FileKind.Pdf, "2026-05-20T08:45:00"),
                MockAttachment("a3", "wireframe-home.png", "image/png", 864_000, FileKind.Image, "2026-05-20T08:45:00", Color(0xFFDBEAFE))
            )
        ),
        MockMessage(
            id = "m3",
            folderId = "work",
            type = MessageType.Document,
            content = "Versão final do escopo aprovada.",
            createdAt = "2026-05-19T16:10:00",
            isFavorite = true,
            syncStatus = SyncStatus.Synced,
            attachments = listOf(
                MockAttachment("a4", "escopo-final.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 442_000, FileKind.Document, "2026-05-19T16:10:00")
            )
        ),
        MockMessage(
            id = "m4",
            folderId = "study",
            type = MessageType.List,
            content = "Leitura da semana",
            createdAt = "2026-05-19T20:30:00",
            isFavorite = false,
            syncStatus = SyncStatus.Synced,
            listItems = listOf(
                MockListItem("l1", "Revisar notas de Kotlin", true),
                MockListItem("l2", "Separar referências de Compose", false),
                MockListItem("l3", "Exportar resumo em PDF", false)
            )
        ),
        MockMessage(
            id = "m5",
            folderId = "images",
            type = MessageType.Image,
            content = "Mockups exportados para comparar no Android.",
            createdAt = "2026-05-18T18:20:00",
            isFavorite = true,
            syncStatus = SyncStatus.Synced,
            attachments = listOf(
                MockAttachment("a5", "dashboard-preview.png", "image/png", 1_240_000, FileKind.Image, "2026-05-18T18:20:00", Color(0xFFE0E7FF)),
                MockAttachment("a6", "empty-state.png", "image/png", 660_000, FileKind.Image, "2026-05-18T18:20:00", Color(0xFFFCE7F3))
            )
        ),
        MockMessage(
            id = "m6",
            folderId = "inbox",
            type = MessageType.Text,
            content = "Ideia: transformar mensagens com anexos em uma visualização de drive.",
            createdAt = "2026-05-18T10:00:00",
            isFavorite = false,
            syncStatus = SyncStatus.Synced
        )
    )

    val files: List<MockFile> = messages.flatMap { message ->
        val channel = channels.first { it.id == message.folderId }
        message.attachments.map {
            MockFile(
                id = it.id,
                name = it.fileName,
                kind = it.kind,
                size = it.size,
                folderId = message.folderId,
                folderName = channel.name,
                messageId = message.id,
                isFavorite = message.isFavorite,
                createdAt = it.createdAt,
                previewColor = it.previewColor
            )
        }
    }
}
