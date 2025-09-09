package com.jsramraj.playmatecompanion.android.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import java.time.LocalTime
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jsramraj.playmatecompanion.android.components.LogViewer
import com.jsramraj.playmatecompanion.android.utils.LogManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.auth.AuthViewModel
import com.jsramraj.playmatecompanion.android.auth.SessionManager
import com.jsramraj.playmatecompanion.android.core.Constants
import com.jsramraj.playmatecompanion.android.sync.DataSyncWorker
import com.jsramraj.playmatecompanion.android.utils.AppInfo
import com.jsramraj.playmatecompanion.database.SyncPreferences


@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onSave: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val sessionInfo = remember { sessionManager.getSessionInfo() }
    val appVersion = remember { AppInfo.getVersionName(context) }
    val logManager = remember { LogManager.getInstance(context) }
    var clubId by remember { mutableStateOf(sessionManager.getSportsClubId() ?: "") }
    var syncEnabled by remember { mutableStateOf<Boolean>(preferencesManager.syncEnabled) }
    var syncTime by remember { mutableStateOf<LocalTime>(preferencesManager.syncTime) }
    var syncInterval by remember { mutableStateOf(preferencesManager.syncIntervalHours) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showIntervalMenu by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    
    val syncIntervalOptions = listOf(1, 2, 4, 6, 8, 12, 24)

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
                        "Settings",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            sessionManager.saveSportsClubId(clubId)
                            preferencesManager.syncEnabled = syncEnabled
                            preferencesManager.syncTime = syncTime
                            preferencesManager.syncIntervalHours = syncInterval
                            DataSyncWorker.updateSchedule(context)
                            onSave()
                        },
                        enabled = clubId.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Club Settings Section
            SectionHeader(text = "Club Settings")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ListItem(
                    headlineContent = {
                        OutlinedTextField(
                            value = clubId,
                            onValueChange = { clubId = it },
                            label = { Text("Club ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Club ID"
                        )
                    }
                )
            }

            // Sync Settings Section
            SectionHeader(text = "Sync Settings")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Sync Enable Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Daily Sync",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { syncEnabled = it }
                    )
                }

                // Sync Time Picker
                if (syncEnabled) {
                    ListItem(
                        modifier = Modifier.clickable { showTimePicker = true },
                        headlineContent = { Text("Sync Time") },
                        supportingContent = {
                            Text(
                                "Daily sync will run at ${
                                    syncTime.format(
                                        java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                                    )
                                }"
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Schedule"
                            )
                        }
                    )

                    // Sync Interval Selector
                    Box {
                        ListItem(
                            modifier = Modifier.clickable { showIntervalMenu = true },
                            headlineContent = { Text("Sync Frequency") },
                            supportingContent = {
                                Text("Every ${syncInterval} hour${if (syncInterval > 1) "s" else ""}")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Interval"
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showIntervalMenu,
                            onDismissRequest = { showIntervalMenu = false }
                        ) {
                            syncIntervalOptions.forEach { hours ->
                                DropdownMenuItem(
                                    text = { Text("Every ${hours} hour${if (hours > 1) "s" else ""}") },
                                    onClick = {
                                        syncInterval = hours
                                        showIntervalMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showTimePicker) {
                TimePickerDialog(
                    onDismiss = { showTimePicker = false },
                    onConfirm = { time -> syncTime = time },
                    initialTime = syncTime
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Account Section
            SectionHeader(text = "Account")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Placeholder for profile image
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column {
                        Text(
                            sessionInfo.userName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            sessionInfo.userEmail ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Google Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = {
                        authViewModel.signOut { 
                            onLogout() 
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("LOGOUT")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // About Section
            SectionHeader(text = "About")
            ListItem(
                headlineContent = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Version",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            appVersion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Version Info"
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Logs Section
            SectionHeader(text = "Logs")
            ListItem(
                modifier = Modifier.clickable { onNavigateToLogs() },
                headlineContent = { Text("View Application Logs") },
                supportingContent = { Text("View detailed application logs and events") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "View Logs"
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate"
                    )
                }
            )
        }
    }
}
