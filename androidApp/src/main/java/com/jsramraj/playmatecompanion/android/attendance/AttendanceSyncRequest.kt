package com.jsramraj.playmatecompanion.android.attendance

data class AttendanceSyncRequest(
    val memberId: Long,
    val checkInTime: String,
    val checkOutTime: String?,
    val date: String
)
