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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.utils.AvatarColorUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceListScreen(
    onBack: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val groupedAttendanceList by attendanceViewModel.groupedAttendanceList.collectAsState()
    
    Scaffold(
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

@Composable
fun AttendanceCard(record: AttendanceWithMember) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val initial = record.memberName.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = remember(initial) { AvatarColorUtil.getColorForLetter(initial) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Attendance details
            Column(modifier = Modifier.weight(1f)) {
                // First row: Member ID and Name
                Text(
                    text = "#${record.attendance.memberId} - ${record.memberName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Second row: Check-in/out times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "In: ${timeFormatter.format(record.attendance.checkInTime)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    record.attendance.checkOutTime?.let { checkOutTime ->
                        Text(
                            text = "Out: ${timeFormatter.format(checkOutTime)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Status indicator
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

            if (isToday && record.attendance.checkOutTime == null) {
                // Green dot for currently in gym (today only)
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(8.dp)
                        .background(
                            color = Color(0xFF4CAF50), // Material Green
                            shape = CircleShape
                        )
                        .align(Alignment.CenterVertically)
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
                        .align(Alignment.CenterVertically)
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
