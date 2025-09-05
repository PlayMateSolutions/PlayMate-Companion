package com.jsramraj.playmatecompanion.android.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit
) {
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
                    IconButton(onClick = onNavigateToSettings) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Main content will go here",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
