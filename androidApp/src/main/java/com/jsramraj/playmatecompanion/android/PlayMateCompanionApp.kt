package com.jsramraj.playmatecompanion.android

import android.app.Application
import com.jsramraj.playmatecompanion.android.database.AppDatabase
import com.jsramraj.playmatecompanion.android.sync.DataSyncWorker

class PlayMateCompanionApp : Application() {
    // Database will be initialized lazily when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize the sync worker based on current settings
        DataSyncWorker.updateSchedule(this)
    }
}
