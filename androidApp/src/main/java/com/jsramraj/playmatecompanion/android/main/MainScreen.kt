package com.jsramraj.playmatecompanion.android.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jsramraj.playmatecompanion.android.auth.SessionManager
import com.jsramraj.playmatecompanion.android.settings.SettingsScreen
import com.jsramraj.playmatecompanion.android.members.MembersScreen
import com.jsramraj.playmatecompanion.android.attendance.AttendanceListScreen
import com.jsramraj.playmatecompanion.android.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var currentScreen by remember { 
        mutableStateOf(
            if (sessionManager.getSportsClubId().isNullOrEmpty()) 
                Screen.Settings 
            else 
                Screen.Main
        ) 
    }

    when (currentScreen) {
        Screen.Settings -> {
            SettingsScreen(
                onLogout = onLogout,
                onSave = { currentScreen = Screen.Main },
                onBack = { 
                    if (!sessionManager.getSportsClubId().isNullOrEmpty()) {
                        currentScreen = Screen.Main
                    }
                }
            )
        }
        Screen.Members -> {
            MembersScreen(
                onBack = { currentScreen = Screen.Main }
            )
        }
        Screen.Attendance -> {
            AttendanceListScreen(
                onBack = { currentScreen = Screen.Main },
                onOpenMembers = { currentScreen = Screen.Members }
            )
        }
        Screen.Main -> {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "PlayMate Companion",
                            color = MaterialTheme.colorScheme.onPrimary
                        ) 
                    },
                    actions = {
                        // History button
                        IconButton(onClick = { /* TODO: Open history */ }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Settings button
                        IconButton(onClick = { currentScreen = Screen.Settings }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            // Main content
            Box(modifier = Modifier.padding(paddingValues)) {
                // TODO: Add main content here
                Text(
                    "Main content will go here",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}}
