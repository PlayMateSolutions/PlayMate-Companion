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

    companion object {
        private const val PREFERENCES_NAME = "playmate_preferences"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_SYNC_TIME = "sync_time"
    }
}
