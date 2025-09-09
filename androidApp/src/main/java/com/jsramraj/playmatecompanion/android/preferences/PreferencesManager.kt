package com.jsramraj.playmatecompanion.android.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var syncEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SYNC_ENABLED, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_SYNC_ENABLED, value) }

    var syncTime: LocalTime
        get() {
            val defaultTime = "21:00" // 9 PM
            val timeStr = sharedPreferences.getString(KEY_SYNC_TIME, defaultTime) ?: defaultTime
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        }
        set(value) = sharedPreferences.edit {
            putString(KEY_SYNC_TIME, value.format(DateTimeFormatter.ofPattern("HH:mm")))
        }

    var lastMemberSyncTime: Long
        get() = sharedPreferences.getLong(KEY_LAST_MEMBER_SYNC, 0)
        set(value) = sharedPreferences.edit { putLong(KEY_LAST_MEMBER_SYNC, value) }

    var lastAttendanceSyncTime: Long
        get() = sharedPreferences.getLong(KEY_LAST_ATTENDANCE_SYNC, 0)
        set(value) = sharedPreferences.edit { putLong(KEY_LAST_ATTENDANCE_SYNC, value) }

    fun getFormattedLastSyncTime(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now" // less than 1 minute
            diff < 3600_000 -> "${diff / 60_000} minutes ago" // less than 1 hour
            diff < 86400_000 -> "${diff / 3600_000} hours ago" // less than 1 day
            else -> "${diff / 86400_000} days ago"
        }
    }

    var syncIntervalHours: Int
        get() = sharedPreferences.getInt(KEY_SYNC_INTERVAL_HOURS, 12) // default 12 hours
        set(value) = sharedPreferences.edit { putInt(KEY_SYNC_INTERVAL_HOURS, value) }

    companion object {
        private const val PREFERENCES_NAME = "playmate_preferences"
        private const val KEY_SYNC_INTERVAL_HOURS = "sync_interval_hours"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_SYNC_TIME = "sync_time"
        private const val KEY_LAST_MEMBER_SYNC = "last_member_sync"
        private const val KEY_LAST_ATTENDANCE_SYNC = "last_attendance_sync"
    }
}
