package com.jsramraj.playmatecompanion.android

import android.app.Application
import com.jsramraj.playmatecompanion.android.database.AppDatabase

class PlayMateCompanionApp : Application() {
    // Database will be initialized lazily when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global components here if needed
    }
}
