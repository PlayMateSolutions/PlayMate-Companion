package com.jsramraj.playmatecompanion.android.main

import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.attendance.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    // Collect attendance state
    val inputText by attendanceViewModel.inputText.collectAsState()
    val message by attendanceViewModel.message.collectAsState()
    val isLoading by attendanceViewModel.isLoading.collectAsState()
    val todayAttendanceCount by attendanceViewModel.todayAttendanceCount.collectAsState()
    
    // Auto-focus for the input field
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Remember if snackbar is showing
    var showSnackbar by remember { mutableStateOf(false) }
    
    // Set focus on the input field when the screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Show snackbar when message changes
    LaunchedEffect(message) {
        if (message != null) {
            showSnackbar = true
        }
    }

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
                    // Today's attendance count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Attendance Today",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$todayAttendanceCount",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
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
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    attendanceViewModel.processAttendance()
                                    focusManager.clearFocus()
                                }
                            ),
                            isError = message?.startsWith("Error") == true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                attendanceViewModel.processAttendance()
                                focusManager.clearFocus()
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
                
                // Additional content can go here
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Snackbar for messages
            if (showSnackbar && message != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Snackbar(
                        action = {
                            TextButton(onClick = {
                                showSnackbar = false
                                attendanceViewModel.clearMessage()
                            }) {
                                Text("Dismiss")
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(text = message ?: "")
                    }
                }
            }
        }
    }
}
