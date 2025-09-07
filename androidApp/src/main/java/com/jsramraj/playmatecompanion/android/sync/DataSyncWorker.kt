package com.jsramraj.playmatecompanion.android.sync

import android.content.Context
import androidx.work.*
import com.jsramraj.playmatecompanion.android.attendance.AttendanceSyncRequest
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import com.jsramraj.playmatecompanion.android.repository.AttendanceRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val memberRepository = MemberRepository(applicationContext)
    private val attendanceRepository = AttendanceRepository(applicationContext)
    private val preferencesManager = PreferencesManager(applicationContext)

    override suspend fun doWork(): Result {
        try {
            android.util.Log.d("DataSyncWorker", "Starting sync work at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            // 1. Fetch and refresh members from server
            val membersResult = memberRepository.refreshMembers()
            if (membersResult.isFailure) {
                return Result.retry()
            }

            // 2. Sync unsynced attendance records
            val syncResult = attendanceRepository.syncUnsynced()
            if (syncResult.isFailure) {
                return Result.retry()
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "data_sync_work"

        fun schedule(context: Context, hour: Int, minute: Int) {
            android.util.Log.d("DataSyncWorker", "Scheduling sync for $hour:$minute")
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
            android.util.Log.d("DataSyncWorker", "Initial delay will be ${initialDelay/1000/60} minutes")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)  // Set the initial delay
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
