package com.jsramraj.playmatecompanion.android.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsramraj.playmatecompanion.android.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AttendanceRepository(application)
    
    // Track the input text for the attendance ID/phone
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    // Track success/error messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    // Track loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Today's attendance count
    val todayAttendanceCount = repository.getTodayAttendanceCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Update input text
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    // Process attendance input
    fun processAttendance() {
        val input = _inputText.value.trim()
        if (input.isEmpty()) {
            _message.value = "Please enter a member ID or phone number"
            return
        }
        
        _isLoading.value = true
        _message.value = null
        
        viewModelScope.launch {
            try {
                // Use the consolidated method that handles both member ID and phone
                val result = repository.logAttendanceByIdentifier(input)
                
                result.fold(
                    onSuccess = { attendance ->
                        val checkOutTime = attendance.checkOutTime
                        if (checkOutTime != null) {
                            _message.value = "Check-out recorded for member #${attendance.memberId}"
                        } else {
                            _message.value = "Check-in recorded for member #${attendance.memberId}"
                        }
                        // Clear the input field on success
                        _inputText.value = ""
                    },
                    onFailure = { exception ->
                        _message.value = "Error: ${exception.message ?: "Unknown error"}"
                    }
                )
            } catch (e: Exception) {
                _message.value = "Error: ${e.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Clear message
    fun clearMessage() {
        _message.value = null
    }
}
