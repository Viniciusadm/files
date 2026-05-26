package com.archieapps.fles.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Url
import okhttp3.ResponseBody

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<LoginResponse>

    @POST("logout")
    suspend fun logout(): ApiEnvelope<Unit>

    @GET("user")
    suspend fun me(): ApiEnvelope<UserResponse>

    @GET("user/secure-folders/meta")
    suspend fun secureMeta(): ApiEnvelope<SecureFolderMetaResponse>

    @POST("user/secure-folders/pin")
    suspend fun setupSecurePin(@Body request: SecurePinSetupRequest): ApiEnvelope<SecureFolderMetaResponse>

    @POST("user/secure-folders/verify")
    suspend fun verifySecurePin(@Body request: SecurePinVerifyRequest): ApiEnvelope<SecureFolderMetaResponse>

    @POST("user/secure-folders/reset")
    suspend fun resetSecurePin(@Body request: SecurePinSetupRequest): ApiEnvelope<SecureFolderMetaResponse>

    @GET("channels")
    suspend fun channels(): ApiEnvelope<List<RemoteChannel>>

    @POST("folders")
    suspend fun createFolder(@Body request: CreateFolderRequest): ApiEnvelope<RemoteChannel>

    @PATCH("folders/{id}")
    suspend fun updateFolder(@Path("id") id: String, @Body request: UpdateFolderRequest): ApiEnvelope<RemoteChannel>

    @DELETE("folders/{id}")
    suspend fun deleteFolder(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("folders/{folderId}/messages")
    suspend fun messages(@Path("folderId") folderId: String): ApiEnvelope<List<RemoteMessage>>

    @POST("messages")
    suspend fun createMessage(@Body request: CreateMessageRequest): ApiEnvelope<RemoteMessage>

    @Multipart
    @POST("messages")
    suspend fun uploadMessage(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): ApiEnvelope<RemoteMessage>

    @POST("uploads")
    suspend fun createUpload(@Body request: CreateUploadRequest): ApiEnvelope<UploadSessionResponse>

    @GET("uploads/{uploadId}")
    suspend fun uploadStatus(@Path("uploadId") uploadId: String): ApiEnvelope<UploadSessionResponse>

    @Multipart
    @POST("uploads/{uploadId}/chunks")
    suspend fun uploadChunk(
        @Path("uploadId") uploadId: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part chunk: MultipartBody.Part
    ): ApiEnvelope<UploadSessionResponse>

    @POST("uploads/{uploadId}/complete")
    suspend fun completeUpload(@Path("uploadId") uploadId: String): ApiEnvelope<UploadSessionResponse>

    @DELETE("uploads/{uploadId}")
    suspend fun cancelUpload(@Path("uploadId") uploadId: String): ApiEnvelope<Unit>

    @PATCH("messages/{id}/move")
    suspend fun moveMessage(
        @Path("id") id: String,
        @Body request: MoveMessageRequest
    ): ApiEnvelope<RemoteMessage>

    @POST("messages/checklists")
    suspend fun createChecklist(@Body request: CreateChecklistRequest): ApiEnvelope<RemoteMessage>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): ApiEnvelope<Unit>

    @PATCH("messages/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: String): ApiEnvelope<RemoteMessage>

    @POST("messages/{messageId}/list-items")
    suspend fun addListItem(
        @Path("messageId") messageId: String,
        @Body request: AddListItemRequest
    ): ApiEnvelope<RemoteListItem>

    @PATCH("messages/{messageId}/list-items/{itemId}")
    suspend fun updateListItem(
        @Path("messageId") messageId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateListItemRequest
    ): ApiEnvelope<RemoteListItem>

    @PATCH("messages/{messageId}/list-items/{itemId}/toggle")
    suspend fun toggleListItem(
        @Path("messageId") messageId: String,
        @Path("itemId") itemId: String
    ): ApiEnvelope<RemoteListItem>

    @PATCH("messages/{messageId}/list-items/reorder")
    suspend fun reorderListItems(
        @Path("messageId") messageId: String,
        @Body request: ReorderListItemsRequest
    ): ApiEnvelope<Unit>

    @DELETE("messages/{messageId}/list-items/{itemId}")
    suspend fun deleteListItem(
        @Path("messageId") messageId: String,
        @Path("itemId") itemId: String
    ): ApiEnvelope<Unit>

    @PATCH("attachments/{id}/rename")
    suspend fun renameAttachment(
        @Path("id") id: String,
        @Body request: RenameAttachmentRequest
    ): ApiEnvelope<RemoteAttachment>

    @DELETE("attachments/{id}")
    suspend fun deleteAttachment(@Path("id") id: String): ApiEnvelope<Unit>

    @PATCH("attachments/{id}/move")
    suspend fun moveAttachment(
        @Path("id") id: String,
        @Body request: MoveAttachmentRequest
    ): ApiEnvelope<AttachmentMutationResponse>

    @PATCH("attachments/{id}/favorite")
    suspend fun favoriteAttachment(@Path("id") id: String): ApiEnvelope<AttachmentMutationResponse>

    @POST("attachments/bulk-delete")
    suspend fun bulkDeleteAttachments(@Body request: BulkAttachmentIdsRequest): ApiEnvelope<BulkAttachmentResult>

    @POST("attachments/bulk-move")
    suspend fun bulkMoveAttachments(@Body request: BulkMoveAttachmentsRequest): ApiEnvelope<BulkAttachmentMoveResult>

    @GET
    suspend fun download(@Url url: String): ResponseBody
}

data class ApiEnvelope<T>(
    val success: Boolean? = null,
    val message: String? = null,
    val body: T? = null,
    val misc: Any? = null,
    val error: Any? = null
)

fun <T> ApiEnvelope<T>.requireBody(): T {
    if (success == false) throw ApiException(null, message ?: "Erro na requisição")
    return body ?: throw ApiException(null, message ?: "Resposta vazia do servidor")
}

fun ApiEnvelope<Unit>.requireSuccess() {
    if (success == false) throw ApiException(null, message ?: "Erro na requisição")
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse? = null
)

data class UserResponse(
    val id: String?,
    val name: String?,
    val email: String
)

data class RemoteChannel(
    val id: String?,
    val name: String?,
    val lastMessage: String?,
    val updatedAt: String?,
    val parentId: String? = null,
    val icon: String?,
    val color: String?,
    val isSecure: Boolean? = null
)

data class RemoteListItem(
    val id: String?,
    val messageId: String?,
    val text: String?,
    val isChecked: Any?,
    val position: Any?,
    val createdAt: String?,
    val updatedAt: String?
)

data class RemoteAttachment(
    val id: String?,
    val messageId: String?,
    val fileName: String?,
    val mimeType: String?,
    val size: Any?,
    val url: String?,
    val previewUrl: String?,
    val compressed: Any?,
    val fileKind: String?,
    val extension: String?,
    val createdAt: String?
)

data class RemoteMessage(
    val id: String?,
    val folderId: String?,
    val folderIsSecure: Boolean? = null,
    val type: String?,
    val content: String?,
    val listItems: List<RemoteListItem>?,
    val attachments: List<RemoteAttachment>?,
    val isFavorite: Any?,
    val createdAt: String?,
    val updatedAt: String?
)

data class CreateUploadRequest(
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val chunkSize: Int,
    val extension: String,
    val fileKind: String,
    val compressed: Boolean
)

data class UploadSessionResponse(
    val uploadId: String,
    val chunkSize: Int,
    val totalChunks: Int,
    val receivedChunks: Int,
    val receivedBytes: Long,
    val nextChunkIndex: Int,
    val size: Long,
    val status: String
)

data class CreateFolderRequest(
    val name: String,
    val parentId: String?,
    val icon: String,
    val color: String,
    val isSecure: Boolean = false
)

data class UpdateFolderRequest(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val parentId: String? = null,
    val isSecure: Boolean? = null
)

data class SecureFolderMetaResponse(
    val configured: Boolean = false,
    val keySalt: String? = null,
    val keyIterations: Int? = null,
    val keyEnvelope: String? = null,
    val keyVersion: Int? = null,
    val pinSetAt: String? = null
)

data class SecurePinSetupRequest(
    val pin: String,
    val keySalt: String,
    val keyIterations: Int,
    val keyEnvelope: String,
    val keyVersion: Int = 1
)

data class SecurePinVerifyRequest(
    val pin: String
)

data class CreateMessageRequest(
    val folderId: String,
    val content: String
)

data class MoveMessageRequest(
    val targetFolderId: String
)

data class RenameAttachmentRequest(
    val name: String
)

data class MoveAttachmentRequest(
    val targetFolderId: String
)

data class BulkAttachmentIdsRequest(
    val ids: List<String>
)

data class BulkMoveAttachmentsRequest(
    val ids: List<String>,
    val targetFolderId: String
)

data class AttachmentMutationResponse(
    val attachment: RemoteAttachment?,
    val messageId: String?,
    val isFavorite: Any? = null
)

data class BulkAttachmentResult(
    val deleted: List<String>? = null,
    val failed: List<String>? = null
)

data class BulkAttachmentMoveResult(
    val moved: List<String>? = null,
    val failed: List<String>? = null,
    val messageId: String? = null
)

data class CreateChecklistRequest(
    val folderId: String,
    val content: String,
    val items: List<ChecklistItemRequest>
)

data class ChecklistItemRequest(
    val text: String
)

data class AddListItemRequest(
    val text: String
)

data class UpdateListItemRequest(
    val text: String? = null,
    val isChecked: Boolean? = null
)

data class ReorderListItemsRequest(
    val orderedItemIds: List<String>
)
