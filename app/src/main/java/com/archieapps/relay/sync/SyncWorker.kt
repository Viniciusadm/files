package com.archieapps.relay.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.archieapps.relay.FlesApp

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as FlesApp).container
        return runCatching {
            container.syncEngine.runOnce()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
