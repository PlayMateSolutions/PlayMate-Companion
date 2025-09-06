package com.jsramraj.playmatecompanion.android.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jsramraj.playmatecompanion.android.attendance.Attendance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String, // ISO 8601 date string: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
    val checkInTime: String, // ISO 8601 date string: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
    val checkOutTime: String?, // ISO 8601 date string: yyyy-MM-dd'T'HH:mm:ss.SSS'Z', nullable
    val daysToExpiry: Int,
    val notes: String?,
    val synced: Boolean // Track if the record has been synced to the server
) {
    companion object {
        fun fromAttendance(attendance: Attendance): AttendanceEntity {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            
            return AttendanceEntity(
                id = attendance.id,
                memberId = attendance.memberId,
                date = dateFormat.format(attendance.date),
                checkInTime = dateFormat.format(attendance.checkInTime),
                checkOutTime = attendance.checkOutTime?.let { dateFormat.format(it) },
                daysToExpiry = attendance.daysToExpiry,
                notes = attendance.notes,
                synced = attendance.synced
            )
        }
    }

    fun toAttendance(): Attendance {
        return Attendance(
            id = id,
            memberId = memberId,
            date = parseIsoDate(date),
            checkInTime = parseIsoDate(checkInTime),
            checkOutTime = checkOutTime?.let { parseIsoDate(it) },
            daysToExpiry = daysToExpiry,
            notes = notes,
            synced = synced
        )
    }
    
    private fun parseIsoDate(dateString: String): Date {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(dateString) ?: Date()
        } catch (e: Exception) {
            // Fallback to current date if parsing fails
            Date()
        }
    }
}
