package com.jsramraj.playmatecompanion.android.sync

import android.content.Context
import androidx.work.*
import androidx.work.WorkManager
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.time.LocalTime

class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // TODO: Implement the sync logic here
            // 1. Fetch members from server
            // 2. Upload local attendance records
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "data_sync_work"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                24, TimeUnit.HOURS,
                initialDelay, TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyWorkRequest
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        fun updateSchedule(context: Context) {
            val preferencesManager = PreferencesManager(context)
            if (preferencesManager.syncEnabled) {
                val syncTime = preferencesManager.syncTime
                schedule(context, syncTime.hour, syncTime.minute)
            } else {
                cancel(context)
            }
        }
    }
}
