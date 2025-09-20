package com.jsramraj.playmatecompanion.android.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsramraj.playmatecompanion.android.repository.AttendanceRepository
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import com.jsramraj.playmatecompanion.android.utils.LogManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import java.util.TimeZone

// Define a data class for the welcome card information
data class WelcomeCardInfo(
    val memberId: Long,
    val memberName: String,
    val isCheckIn: Boolean,
    val timestamp: Date,
    val daysToExpiry: Int? = null
)

// Data class for grouped attendance records
data class AttendanceGroup(
    val date: Date,
    val records: List<AttendanceWithMember>
)

// Data class to hold attendance with member info
data class AttendanceWithMember(
    val attendance: Attendance,
    var memberName: String = "" // Will be populated from member repository
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AttendanceRepository(application)
    private val memberRepository = MemberRepository(application)
    private val logManager = LogManager.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    // Track the input text for the attendance ID/phone
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    // Track success/error messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Track error details
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Track loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Track syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun syncAttendance() {
        if (_isSyncing.value) {
            logManager.w("Attendance", "Sync already in progress, skipping request")
            return
        }
        
        logManager.i("Attendance", "Starting attendance sync")
        _isSyncing.value = true
        
        viewModelScope.launch {
            try {
                repository.syncUnsynced()
                logManager.i("Attendance", "Attendance sync completed successfully")
                _error.value = null
            } catch (e: Exception) {
                logManager.e("Attendance", "Sync failed: ${e.message}")
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    // Track the currently shown welcome message with member details
    private val _welcomeInfo = MutableStateFlow<WelcomeCardInfo?>(null)
    val welcomeInfo: StateFlow<WelcomeCardInfo?> = _welcomeInfo
    
    // Today's attendance count
    val todayAttendanceCount = repository.getTodayAttendanceCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Grouped attendance list
    val groupedAttendanceList: StateFlow<List<AttendanceGroup>> = repository.getAllAttendance()
        .transformLatest { attendanceList ->
            logManager.i("Attendance", "Loading attendance records. Total records: ${attendanceList.size}")
            
            val withMembers = coroutineScope {
                attendanceList.map { attendance ->
                    async {
                        val member = memberRepository.getMemberById(attendance.memberId)
                        if (member == null) {
                            logManager.w("Attendance", "Member not found for attendance record. Member ID: ${attendance.memberId}")
                        }
                        AttendanceWithMember(
                            attendance = attendance,
                            memberName = if (member != null) "${member.firstName} ${member.lastName}" else "Unknown"
                        )
                    }
                }.awaitAll()
            }

            val grouped = withMembers
                .groupBy { record ->
                    val cal = Calendar.getInstance()
                    cal.time = record.attendance.date
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.time
                }
                .map { (date, records) ->
                    AttendanceGroup(date, records)
                }
                .sortedByDescending { it.date }

            emit(grouped)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Update input text
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    // Process attendance input
    fun processAttendance() {
        val input = _inputText.value.trim()
        if (input.isEmpty()) {
            logManager.w("Attendance", "Empty input received")
            _message.value = "Please enter a member ID or phone number"
            return
        }
        
        logManager.i("Attendance", "Processing attendance for input: $input")
        _isLoading.value = true
        _message.value = null
        
        viewModelScope.launch {
            try {
                val result = repository.logAttendanceByIdentifier(input)
                
                result.fold(
                    onSuccess = { attendance ->
                        val member = memberRepository.getMemberById(attendance.memberId)
                        if (member != null) {
                            val checkOutTime = attendance.checkOutTime
                            val isCheckIn = checkOutTime == null
                            val action = if (isCheckIn) "Check-in" else "Check-out"
                            
                            logManager.i("Attendance", "$action recorded for ${member.firstName} ${member.lastName} (ID: ${member.id})")
                            
                            val expiryDate = member.expiryDate
                            val today = Date()
                            val daysToExpiry = if (expiryDate != null) {
                                val diff = expiryDate.time - today.time
                                (diff / (1000 * 60 * 60 * 24)).toInt()
                            } else null
                            _welcomeInfo.value = WelcomeCardInfo(
                                memberId = member.id,
                                memberName = "${member.firstName} ${member.lastName}",
                                isCheckIn = isCheckIn,
                                timestamp = if (isCheckIn) attendance.checkInTime else checkOutTime!!,
                                daysToExpiry = daysToExpiry
                            )
                            
                            if (isCheckIn) {
                                _message.value = "Welcome, ${member.firstName}! Check-in recorded."
                            } else {
                                _message.value = "Goodbye, ${member.firstName}! Check-out recorded."
                            }
                        } else {
                            logManager.w("Attendance", "Attendance recorded for unknown member ID: ${attendance.memberId}")
                            _message.value = "Attendance recorded for member #${attendance.memberId}"
                        }
                        
                        _inputText.value = ""
                    },
                    onFailure = { exception ->
                        logManager.e("Attendance", "Failed to process attendance: ${exception.message}")
                        setErrorMessage("Error: ${exception.message ?: "Unknown error"}")
                    }
                )
            } catch (e: Exception) {
                setErrorMessage("Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Clear welcome info
    fun clearWelcomeInfo() {
        _welcomeInfo.value = null
    }
    
    // Set error message with auto-clear after 5 seconds
    private fun setErrorMessage(message: String) {
        _message.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // 5 seconds
            if (_message.value == message) { // Only clear if the message hasn't changed
                _message.value = null
            }
        }
    }

    // Clear message
    fun clearMessage() {
        _message.value = null
    }
}
