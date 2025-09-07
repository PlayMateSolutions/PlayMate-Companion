package com.jsramraj.playmatecompanion.android.sync

import android.content.Context
import androidx.work.*
import com.jsramraj.playmatecompanion.android.attendance.AttendanceSyncRequest
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import com.jsramraj.playmatecompanion.android.repository.AttendanceRepository
import com.jsramraj.playmatecompanion.android.utils.LogManager
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
    private val logManager = LogManager.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        val syncStartTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logManager.i("DataSync", "Starting scheduled sync operation at $syncStartTime")

        try {
            // 1. Fetch and refresh members from server
            logManager.i("DataSync", "Starting member list refresh")
            val membersResult = memberRepository.refreshMembers()
            if (membersResult.isFailure) {
                val error = membersResult.exceptionOrNull()
                logManager.e("DataSync", "Member sync failed: ${error?.message ?: "Unknown error"}")
                return Result.retry()
            }
            
            membersResult.getOrNull()?.let { members ->
                logManager.i("DataSync", "Member sync successful. Updated ${members.size} members")
            }

            // 2. Sync unsynced attendance records
            logManager.i("DataSync", "Starting attendance sync")
            val syncResult = attendanceRepository.syncUnsynced()
            if (syncResult.isFailure) {
                val error = syncResult.exceptionOrNull()
                logManager.e("DataSync", "Attendance sync failed: ${error?.message ?: "Unknown error"}")
                return Result.retry()
            }

            val syncEndTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            logManager.i("DataSync", "Sync operation completed successfully at $syncEndTime")
            return Result.success()
        } catch (e: Exception) {
            logManager.e("DataSync", "Unexpected error during sync: ${e.message ?: "Unknown error"}")
            return Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "data_sync_work"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val logManager = com.jsramraj.playmatecompanion.android.utils.LogManager.getInstance(context)
            logManager.i("DataSync", "Scheduling daily sync for $hour:$minute")
            
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
            val scheduledTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
            logManager.i("DataSync", "Next sync scheduled for: $scheduledTime (in ${initialDelay/1000/60} minutes)")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(100, TimeUnit.MILLISECONDS)  // Set the initial delay
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyWorkRequest
                )
        }

        fun cancel(context: Context) {
            val logManager = com.jsramraj.playmatecompanion.android.utils.LogManager.getInstance(context)
            logManager.i("DataSync", "Cancelling scheduled sync work")
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        fun updateSchedule(context: Context) {
            val logManager = com.jsramraj.playmatecompanion.android.utils.LogManager.getInstance(context)
            val preferencesManager = PreferencesManager(context)
            if (preferencesManager.syncEnabled) {
                val syncTime = preferencesManager.syncTime
                logManager.i("DataSync", "Updating sync schedule to ${syncTime.hour}:${syncTime.minute}")
                schedule(context, syncTime.hour, syncTime.minute)
            } else {
                logManager.i("DataSync", "Sync disabled, cancelling scheduled work")
                cancel(context)
            }
        }
    }
}
