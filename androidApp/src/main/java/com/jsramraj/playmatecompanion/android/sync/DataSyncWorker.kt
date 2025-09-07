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
                        // 1. Fetch and refresh members from server
            val membersResult = memberRepository.refreshMembers()
            if (membersResult.isFailure) {
                return Result.retry()
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            // 2. Get unsynced attendance records and sync them
            val unsynced = attendanceRepository.getUnsyncedAttendance()
            unsynced.first().let { attendanceList ->
                if (attendanceList.isNotEmpty()) {
                    // Convert attendance records to sync requests
                    val syncRequests = attendanceList.map { attendance ->
                        AttendanceSyncRequest(
                            memberId = attendance.memberId,
                            checkInTime = dateFormat.format(attendance.checkInTime),
                            checkOutTime = attendance.checkOutTime?.let { time -> dateFormat.format(time) },
                            date = dateFormat.format(attendance.date)
                        )
                    }
                    
                    // Sync attendance records with server
                    val syncResult = attendanceRepository.syncAttendance(syncRequests)
                    if (syncResult.isFailure) {
                        return Result.retry()
                    }
                }
            }

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
