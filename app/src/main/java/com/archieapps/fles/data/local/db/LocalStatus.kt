package com.archieapps.fles.data.local.db

object LocalSyncStatus {
    const val Synced = "synced"
    const val PendingCreate = "pending_create"
    const val PendingUpdate = "pending_update"
    const val PendingDelete = "pending_delete"
    const val PendingUpload = "pending_upload"
    const val Uploading = "uploading"
    const val Downloading = "downloading"
    const val Failed = "failed"
}

object LocalUploadStatus {
    const val PendingUpload = "pending_upload"
    const val Uploading = "uploading"
    const val Uploaded = "uploaded"
    const val Failed = "failed"
}

object QueueStatus {
    const val Queued = "queued"
    const val InFlight = "in_flight"
    const val Failed = "failed"
    const val Done = "done"
}
