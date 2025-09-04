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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var showSettings by remember { mutableStateOf(sessionManager.getSportsClubId().isNullOrEmpty()) }

    if (showSettings) {
        SettingsScreen(
            onLogout = onLogout,
            onSave = { showSettings = false },
            onBack = { 
                if (!sessionManager.getSportsClubId().isNullOrEmpty()) {
                    showSettings = false 
                }
            }
        )
    } else {
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
                        IconButton(onClick = { showSettings = true }) {
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
}
