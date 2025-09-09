package com.jsramraj.playmatecompanion.android

import android.app.Application
import com.jsramraj.playmatecompanion.android.database.AppDatabase
import com.jsramraj.playmatecompanion.android.sync.DataSyncWorker
import com.jsramraj.playmatecompanion.android.auth.AuthViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayMateCompanionApp : Application() {
    // Database will be initialized lazily when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AuthViewModel and check for existing sign-in
        val authViewModel = AuthViewModel()
        // Launch a coroutine to call the suspend function
        GlobalScope.launch {
            authViewModel.initialize(this@PlayMateCompanionApp)
        }
    }
}
