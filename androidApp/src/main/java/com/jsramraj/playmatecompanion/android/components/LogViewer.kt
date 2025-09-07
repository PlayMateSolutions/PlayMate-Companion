package com.jsramraj.playmatecompanion.android.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jsramraj.playmatecompanion.android.utils.LogManager

@Composable
fun LogViewer(
    logManager: LogManager,
    modifier: Modifier = Modifier
) {
    val logs by logManager.logs.collectAsState()
    val lazyListState = rememberLazyListState()
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Application Logs",
                style = MaterialTheme.typography.titleMedium
            )
            
            IconButton(onClick = { logManager.clearLogs() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear logs"
                )
            }
        }

        Divider()

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No logs available")
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shadowElevation = 1.dp,
                shape = MaterialTheme.shapes.small
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    state = lazyListState
                ) {
                    item {
                        Text(
                            text = logs,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
