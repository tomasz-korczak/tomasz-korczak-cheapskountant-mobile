package pl.cheapskountant.mobile.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pl.cheapskountant.mobile.CheapskountantApp

object OutboxSync {
    const val NAME = "outbox-sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
    }
}

class OutboxSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as CheapskountantApp).container
        var guard = 0
        while (guard < 20) {
            val receipts = container.receipts.syncWaiting()
            val expenses = container.expenses.syncWaiting()
            if (!receipts && !expenses) break
            guard += 1
        }
        return Result.success()
    }
}
