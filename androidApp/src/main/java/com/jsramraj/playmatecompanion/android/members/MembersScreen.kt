package com.jsramraj.playmatecompanion.android.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsramraj.playmatecompanion.android.R
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.jsramraj.playmatecompanion.android.utils.MembershipStatusUtil
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onBack: () -> Unit,
    viewModel: MembersViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var lastSyncText by remember {
        mutableStateOf(
            preferencesManager.getFormattedLastSyncTime(
                preferencesManager.lastMemberSyncTime
            )
        )
    }

    // Update last sync text every minute
    LaunchedEffect(Unit) {
        while (true) {
            lastSyncText =
                preferencesManager.getFormattedLastSyncTime(preferencesManager.lastMemberSyncTime)
            kotlinx.coroutines.delay(60_000) // Update every minute
        }
    }

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
                    Column {
                        Text(
                            "Members",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (members.isNotEmpty()) {
                            Text(
                                "Showing ${members.size} members",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMembers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isLoading),
                    onRefresh = { viewModel.refreshMembers() }
                ) {
                    if (error != null) {
                        // Error state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "Unknown error occurred",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshMembers() }) {
                                Text("Retry")
                            }
                        }
                    } else if (members.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No members found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pull down to refresh or tap the refresh button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshMembers() }) {
                                Text("Refresh Now")
                            }
                        }
                    } else {
                        // Member list with sorting options
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sorting options
                            val currentSortOption by viewModel.sortOption.collectAsState()
                            val currentSortDirection by viewModel.sortDirection.collectAsState()
                            val primaryRed = Color(0xFFdc2626)

                            // Sort options row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Sort by:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Sort buttons in a row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Sort buttons
                                    SortOption.values().forEach { option ->
                                        Button(
                                            onClick = { viewModel.setSortOption(option) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (currentSortOption == option) primaryRed else Color.LightGray,
                                                contentColor = if (currentSortOption == option) Color.White else Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp) // Perfect pill shape
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = option.displayName,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                // Only show arrow for the currently selected option
                                                if (currentSortOption == option) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (currentSortDirection == SortDirection.ASCENDING) "↑" else "↓",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Member list
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(members) { member ->
                                    MemberCard(member = member)
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MemberCard(member: Member) {
        val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

        // Determine expiry status for color highlighting
        val now = remember { System.currentTimeMillis() }
        val expiryDate = member.expiryDate.time
        val daysUntilExpiry = ((expiryDate - now) / (1000 * 60 * 60 * 24)).toInt()

        // Get status color from utility
        val statusColor = MembershipStatusUtil.getStatusColor(daysUntilExpiry)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Vertical status indicator strip
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(statusColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ID number in first column
                    Column(
                        modifier = Modifier.width(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${member.id}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Member details
                    Column(modifier = Modifier.weight(1f)) {
                        // Name
                        Text(
                            text = "${member.firstName} ${member.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Phone number
                        if (member.phone.isNotEmpty()) {
                            Text(
                                text = "Phone: ${member.phone}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Expiry date
                        Text(
                            text = "Expires: ${dateFormatter.format(member.expiryDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }


