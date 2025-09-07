package com.jsramraj.playmatecompanion.android.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.utils.AvatarColorUtil
import com.jsramraj.playmatecompanion.android.utils.MembershipStatusUtil
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceListScreen(
    onBack: () -> Unit,
    onOpenMembers: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val groupedAttendanceList by attendanceViewModel.groupedAttendanceList.collectAsState()
    val errorMessage by attendanceViewModel.error.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var lastSyncText by remember {
        mutableStateOf(
            preferencesManager.getFormattedLastSyncTime(
                preferencesManager.lastAttendanceSyncTime
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Update last sync text every minute
    LaunchedEffect(Unit) {
        while (true) {
            lastSyncText =
                preferencesManager.getFormattedLastSyncTime(preferencesManager.lastAttendanceSyncTime)
            kotlinx.coroutines.delay(60_000) // Update every minute
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Attendance History",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Showing ${groupedAttendanceList.sumOf { group -> group.records.size }} records",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    // Members list button
                    IconButton(onClick = onOpenMembers) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "View Members",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Sync button
                    val isSyncing by attendanceViewModel.isSyncing.collectAsState()
                    IconButton(
                        onClick = { attendanceViewModel.syncAttendance() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = "Sync attendance",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
                .padding(paddingValues)
        ) {
            // Sync Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Last synced: $lastSyncText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

                        // Main Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedAttendanceList.forEachIndexed { _, attendanceGroup: AttendanceGroup ->
                    item(key = "header-${attendanceGroup.date.time}") {
                        // Date header
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                                .format(attendanceGroup.date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(
                        items = attendanceGroup.records,
                        key = { record: AttendanceWithMember ->
                            "${record.attendance.id}-${record.attendance.checkInTime.time}"
                        }
                    ) { record: AttendanceWithMember ->
                        AttendanceCard(record = record)
                    }
                }
            }
        }
    }
}

    @Composable
    fun AttendanceCard(record: AttendanceWithMember) {
        val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
        val initial = record.memberName.firstOrNull()?.uppercaseChar() ?: '?'
        val avatarColor = remember(initial) { AvatarColorUtil.getColorForLetter(initial) }

        val isToday = remember(record.attendance.date) {
            val cal1 = Calendar.getInstance()
            cal1.time = record.attendance.date
            cal1.set(Calendar.HOUR_OF_DAY, 0)
            cal1.set(Calendar.MINUTE, 0)
            cal1.set(Calendar.SECOND, 0)
            cal1.set(Calendar.MILLISECOND, 0)

            val cal2 = Calendar.getInstance()
            cal2.set(Calendar.HOUR_OF_DAY, 0)
            cal2.set(Calendar.MINUTE, 0)
            cal2.set(Calendar.SECOND, 0)
            cal2.set(Calendar.MILLISECOND, 0)

            cal1.timeInMillis == cal2.timeInMillis
        }

        // Determine expiry status for color highlighting
        val daysUntilExpiry = record.attendance.daysToExpiry

        // Get status color from utility
        val statusColor = MembershipStatusUtil.getStatusColor(daysUntilExpiry)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 1.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left vertical line - color indicates membership status
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(statusColor)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Member initial with background
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = avatarColor
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = record.attendance.memberId.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Attendance details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Member name and status dot in first column
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = record.memberName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (record.attendance.checkOutTime == null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isToday) {
                                        // Green dot for currently in gym (today only)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = Color(0xFF4CAF50), // Material Green
                                                    shape = CircleShape
                                                )
                                        )
                                    } else {
                                        // Amber dot for missing checkout (past dates)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = Color(0xFFFFA000), // Material Amber
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }

                            // Cloud sync icon for unsynced records
                            if (!record.attendance.synced) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Not synced",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Check-in/out times
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "In: ${timeFormatter.format(record.attendance.checkInTime)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            record.attendance.checkOutTime?.let { checkOutTime ->
                                Text(
                                    text = "Out: ${timeFormatter.format(checkOutTime)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Missing checkout warning for past dates
                        if (!isToday && record.attendance.checkOutTime == null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .background(
                                        color = Color(0xFFFFA000), // Material Amber
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .background(
                                        color = Color(0xFF4CAF50), // Material Green
                                        shape = CircleShape
                                    )
                            )
                        } else if (!isToday && record.attendance.checkOutTime == null) {
                            // Amber dot for missing checkout (past dates)
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .background(
                                        color = Color(0xFFFFA000), // Material Amber
                                        shape = CircleShape
                                    )
                            )
                        } else {
                            // No indicator for checked out records
                            Spacer(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .width(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }