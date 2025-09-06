package com.jsramraj.playmatecompanion.android.attendance

data class AttendanceSyncResult(
    val success: Boolean,
    val index: Int,
    val error: String?
)

data class AttendanceSyncResponse(
    val successCount: Int,
    val failureCount: Int,
    val results: List<AttendanceSyncResult>
)
