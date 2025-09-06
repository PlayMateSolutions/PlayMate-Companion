package com.jsramraj.playmatecompanion.android.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.attendance.AttendanceViewModel
import java.util.Locale
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAttendanceList: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    // Collect attendance state
    val inputText by attendanceViewModel.inputText.collectAsState()
    val message by attendanceViewModel.message.collectAsState()
    val isLoading by attendanceViewModel.isLoading.collectAsState()
    val todayAttendanceCount by attendanceViewModel.todayAttendanceCount.collectAsState()
    
    // Auto-focus for the input field
    val focusRequester = remember { FocusRequester() }
    
    // Remember if snackbar is showing
    var showSnackbar by remember { mutableStateOf(false) }
    
    // Set focus on the input field when the screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Show snackbar when error message changes
    LaunchedEffect(message) {
        if (message != null && message?.startsWith("Error") == true) {
            showSnackbar = true
        }
        // Always request focus to keep the input field active
        focusRequester.requestFocus()
    }
    
    // Keep the screen on while the app is active
    KeepScreenOn()

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
                    // Today's attendance count - clickable to navigate to attendance list
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onNavigateToAttendanceList() }
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "View Attendance List",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$todayAttendanceCount",
                            color = MaterialTheme.colorScheme.onPrimary
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Attendance input section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Log Attendance",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { attendanceViewModel.updateInputText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            label = { Text("Member ID or Phone Number") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Member")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    attendanceViewModel.processAttendance()
                                    // Keep focus on the input field for the next entry
                                    focusRequester.requestFocus()
                                }
                            ),
                            isError = message?.startsWith("Error") == true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                attendanceViewModel.processAttendance()
                                // Keep focus on the input field for the next entry
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(25.dp),
                            enabled = !isLoading && inputText.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Log Attendance")
                            }
                        }
                    }
                }
                
                // Welcome card for the member who just logged in
                val welcomeInfo by attendanceViewModel.welcomeInfo.collectAsState()
                
                // Auto-dismiss welcome card after 5 seconds
                LaunchedEffect(welcomeInfo) {
                    if (welcomeInfo != null) {
                        kotlinx.coroutines.delay(5000) // 5 seconds
                        attendanceViewModel.clearWelcomeInfo()
                        focusRequester.requestFocus()
                    }
                }
                
                if (welcomeInfo != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (welcomeInfo!!.isCheckIn) Color(0xFFE0F7FA) else Color(0xFFFFECB3)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (welcomeInfo!!.isCheckIn) 
                                    "Welcome, ${welcomeInfo!!.memberName}!" 
                                else 
                                    "Goodbye, ${welcomeInfo!!.memberName}!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Member ID: ${welcomeInfo!!.memberId}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            
                            val dateFormat = remember { java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                            Text(
                                text = if (welcomeInfo!!.isCheckIn) 
                                    "Check-in time: ${dateFormat.format(welcomeInfo!!.timestamp)}"
                                else
                                    "Check-out time: ${dateFormat.format(welcomeInfo!!.timestamp)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { 
                                        attendanceViewModel.clearWelcomeInfo()
                                        // Ensure focus is back on the input field
                                        focusRequester.requestFocus()
                                    }
                                ) {
                                    Text("DISMISS")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Message display (above the keyboard) - only show error messages here
            if (showSnackbar && message != null && message?.startsWith("Error") == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFDAD6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message ?: "",
                                modifier = Modifier.weight(1f),
                                color = Color.Red
                            )
                            TextButton(
                                onClick = {
                                    showSnackbar = false
                                    attendanceViewModel.clearMessage()
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// Helper extension to unwrap Context to Activity
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
