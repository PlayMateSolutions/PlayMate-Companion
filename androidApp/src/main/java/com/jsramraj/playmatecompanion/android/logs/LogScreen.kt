package com.jsramraj.playmatecompanion.android.logs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jsramraj.playmatecompanion.android.components.LogViewer
import com.jsramraj.playmatecompanion.android.utils.LogManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val logManager = remember { LogManager.getInstance(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                title = {
                    Text(
                        "Application Logs",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LogViewer(
                logManager = logManager,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
