package com.example.tesisv3.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tesisv3.data.AppDatabase

class DailyResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).medicationDao()
        return try {
            dao.updateAllStatus("Due")
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
