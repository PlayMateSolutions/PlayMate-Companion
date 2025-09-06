package com.jsramraj.playmatecompanion.android.attendance

import android.util.Log
import com.google.gson.annotations.SerializedName
import java.util.Date

data class Attendance(
    val id: Long,
    val memberId: Long,
    @SerializedName("date")
    val date: Date,
    @SerializedName("checkInTime")
    val checkInTime: Date,
    @SerializedName("checkOutTime")
    val checkOutTime: Date? = null,
    val daysToExpiry: Int,
    val notes: String? = null,
    val synced: Boolean // Track if the record has been synced to the server
)

// Request model for attendance logging
data class AttendanceLogRequest(
    val memberId: Long,
    val date: String, // Keep as String for API serialization
    val checkInTime: String, // Keep as String for API serialization
    val checkOutTime: String? = null, // Keep as String for API serialization
    val notes: String? = null
)
