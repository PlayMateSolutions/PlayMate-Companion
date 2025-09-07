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

    suspend fun syncAttendance(records: List<AttendanceSyncRequest>): Result<Boolean> {
        if (records.isEmpty()) {
            preferencesManager.lastAttendanceSyncTime = System.currentTimeMillis()
            return Result.success(true)
        }
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
    
    // Get unsynced attendance records
    fun getUnsyncedAttendance(): Flow<List<Attendance>> {
        return attendanceDao.getUnsyncedAttendance()
            .map { entities ->
                entities.map { it.toAttendance() }
            }
    }
    
    // Get attendance for a specific member
    fun getAttendanceByMemberId(memberId: Int): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceByMemberId(memberId)
            .map { entities ->
                entities.map { it.toAttendance() }
            }
    }
    
    // Log attendance (check-in or check-out)
    suspend fun logAttendance(memberId: Long): Result<Attendance> = withContext(Dispatchers.IO) {
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
            val existingAttendance = attendanceDao.getAttendanceForMemberOnDate(memberId, todayDatePrefix)
            
            if (existingAttendance != null) {
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
                    notes = "",
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
                return@withContext logAttendance(memberId)
            } else {
                return@withContext Result.failure(
                    Exception("Member not found with identifier: $identifier")
                )
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
