package com.archieapps.fles.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.archieapps.fles.di.AppContainer

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        return runCatching {
            container.syncEngine.runOnce()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
