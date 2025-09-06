package com.jsramraj.playmatecompanion.android.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long
    
    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)
    
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>
    
    @Query("SELECT * FROM attendance WHERE synced = 0 ORDER BY date ASC")
    fun getUnsyncedAttendance(): Flow<List<AttendanceEntity>>
    
    @Query("SELECT * FROM attendance WHERE memberId = :memberId ORDER BY date DESC")
    fun getAttendanceByMemberId(memberId: Int): Flow<List<AttendanceEntity>>
    
    @Query("SELECT * FROM attendance WHERE date LIKE :datePrefix || '%' AND memberId = :memberId LIMIT 1")
    suspend fun getAttendanceForMemberOnDate(memberId: Long, datePrefix: String): AttendanceEntity?
    
    @Query("UPDATE attendance SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
    
    @Query("SELECT COUNT(*) FROM attendance WHERE date LIKE :datePrefix || '%'")
    fun getAttendanceCountForDate(datePrefix: String): Flow<Int>
}
