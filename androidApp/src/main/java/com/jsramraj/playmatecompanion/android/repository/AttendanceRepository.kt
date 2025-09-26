package com.jsramraj.playmatecompanion.android.repository

import android.content.Context
import com.jsramraj.playmatecompanion.android.attendance.Attendance
import com.jsramraj.playmatecompanion.android.attendance.AttendanceSyncRequest
import com.jsramraj.playmatecompanion.android.database.AppDatabase
import com.jsramraj.playmatecompanion.android.database.AttendanceEntity
import com.jsramraj.playmatecompanion.android.network.NetworkHelper
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AttendanceRepository(private val context: Context) {
    private val attendanceDao = AppDatabase.getDatabase(context).attendanceDao()
    private val memberDao = AppDatabase.getDatabase(context).memberDao()
    private val memberRepository = MemberRepository(context)
    private val networkHelper = NetworkHelper(context)
    private val preferencesManager = PreferencesManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun syncUnsynced(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Get unsynced attendance records
            val unsynced = getUnsyncedAttendance()
            if (unsynced.isEmpty()) {
                return@withContext Result.success(true)
            }

            // Convert to sync requests
            val syncRequests = unsynced.map { attendance ->
                AttendanceSyncRequest(
                    memberId = attendance.memberId,
                    checkInTime = dateFormat.format(attendance.checkInTime),
                    checkOutTime = attendance.checkOutTime?.let { time -> dateFormat.format(time) },
                    date = dateFormat.format(attendance.date)
                )
            }

            // Perform sync
            return@withContext networkHelper.syncAttendance(syncRequests).also { result ->
                if (result.isSuccess) {
                    // Update local records as synced
                    val memberIds = syncRequests.map { it.memberId.toLong() }
                    attendanceDao.updateSyncStatus(memberIds, true)
                    preferencesManager.lastAttendanceSyncTime = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Internal method for direct sync calls (if needed)
    internal suspend fun syncAttendance(records: List<AttendanceSyncRequest>): Result<Boolean> {
        return networkHelper.syncAttendance(records).also { result ->
            if (result.isSuccess) {
                // Update local records as synced
                val memberIds = records.map { it.memberId.toLong() }
                attendanceDao.updateSyncStatus(memberIds, true)
                preferencesManager.lastAttendanceSyncTime = System.currentTimeMillis()
            }
        }
    }
    
    // Get all attendance records as a Flow
    fun getAllAttendance(): Flow<List<Attendance>> {
        return attendanceDao.getAllAttendance()
            .map { entities ->
                entities.map { it.toAttendance() }
            }
    }
    
    // Get unsynced attendance records as Flow (for UI updates)
    fun getUnsyncedAttendanceFlow(): Flow<List<Attendance>> {
        return attendanceDao.getUnsyncedAttendanceFlow()
            .map { entities ->
                entities.map { it.toAttendance() }
            }
    }
    
    // Get unsynced attendance records directly (for sync operations)
    suspend fun getUnsyncedAttendance(): List<Attendance> {
        return attendanceDao.getUnsyncedAttendance().map { it.toAttendance() }
    }
    
    // Get attendance for a specific member
    fun getAttendanceByMemberId(memberId: Int): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceByMemberId(memberId)
            .map { entities ->
                entities.map { it.toAttendance() }
            }
    }
    
    // Log attendance (check-in or check-out)
    suspend fun logAttendance(memberId: Long, notes: String): Result<Attendance> = withContext(Dispatchers.IO) {
        try {
            // Verify member exists
            val member = memberDao.getMemberById(memberId)
                ?: return@withContext Result.failure(Exception("Member not found"))
            
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getDefault()
            
            // Today's date in yyyy-MM-dd format to find if there's an existing check-in
            val todayDatePrefix = dateFormat.format(now)
            
            // Check if member already checked in today
            val existingAttendance = attendanceDao.getAttendanceForMemberOnDate(memberId, todayDatePrefix, notes)
            
            if (existingAttendance != null) {
                // Ignore if the last check-in is less than 1 minute ago
                val lastCheckInTime = existingAttendance.checkInTime
                val lastCheckInDate = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(lastCheckInTime)
                } catch (e: Exception) {
                    null
                }
                if (lastCheckInDate != null) {
                    val diffMillis = now.time - lastCheckInDate.time
                    if (diffMillis < 10_000) {
                        // Ignore if less than 10 seconds old
                        return@withContext Result.failure(Exception("Check-out ignored: last check-in was less than 1 minute ago."))
                    }
                }
                // Member already checked in, update check-out time
                val updatedAttendance = existingAttendance.copy(
                    checkOutTime = formatDate(now),
                    synced = false
                )
                attendanceDao.updateAttendance(updatedAttendance)
                return@withContext Result.success(updatedAttendance.toAttendance())
            } else {
                // New check-in
                val attendance = AttendanceEntity(
                    memberId = memberId,
                    date = formatDate(now),
                    checkInTime = formatDate(now),
                    checkOutTime = null,
                    daysToExpiry = calculateDaysToExpiry(member.expiryDate),
                    notes = notes,
                    synced = false
                )
                val id = attendanceDao.insertAttendance(attendance)
                return@withContext Result.success(attendance.copy(id = id).toAttendance())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Main method to log attendance by identifier (ID or phone)
    suspend fun logAttendanceByIdentifier(identifier: String): Result<Attendance> = withContext(Dispatchers.IO) {
        try {
            // Use the MemberRepository to find a member by either ID or phone number
            val memberIdResult = memberRepository.findMemberIdByIdentifier(identifier)
            
            if (memberIdResult.isSuccess) {
                val memberId = memberIdResult.getOrThrow()
                return@withContext logAttendance(memberId, "")
            } else {
                val unknownResult = logAttendance(-1, identifier)
                return@withContext if (unknownResult.isSuccess) {
                    Result.failure(
                        Exception("Hello $identifier! We couldn't find your details. Your attendance has been logged as a guest. Please contact the admin to update your information.")
                    )
                } else {
                    unknownResult
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error logging attendance: ${e.message}"))
        }
    }
    
    // Get today's attendance count
    fun getTodayAttendanceCount(): Flow<Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        val todayDatePrefix = dateFormat.format(Date())
        
        return attendanceDao.getAttendanceCountForDate(todayDatePrefix)
    }
    
    // Mark attendance records as synced
    suspend fun markAttendanceAsSynced(ids: List<Long>) = withContext(Dispatchers.IO) {
        attendanceDao.markAsSynced(ids)
    }
    
    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

    private fun calculateDaysToExpiry(expiryDate: Date): Int {
        val today = Date()
        
        // Calculate difference in milliseconds
        val diffInMillis = expiryDate.time - today.time
        // Convert to days
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
