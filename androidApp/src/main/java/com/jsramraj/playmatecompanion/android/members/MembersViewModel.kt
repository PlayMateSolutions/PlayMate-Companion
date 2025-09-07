package com.jsramraj.playmatecompanion.android.members

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import com.jsramraj.playmatecompanion.android.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Define sort options
enum class SortOption(val displayName: String) {
    ID("No"),
    NAME("Name"),
    EXPIRY_DATE("Expiry")
}

// Sort direction
enum class SortDirection {
    ASCENDING,
    DESCENDING
}

class MembersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemberRepository(application)
    private val logManager = LogManager.getInstance(application)
    
    // Current sort option
    private val _sortOption = MutableStateFlow(SortOption.ID)
    val sortOption: StateFlow<SortOption> = _sortOption
    
    // Current sort direction
    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection
    
    // Stream of members from the database, sorted according to the current sort option and direction
    val members: StateFlow<List<Member>> =
            combine(
                repository.getAllMembers().catch { 
                    logManager.e("MembersViewModel", "Error fetching members: ${it.message}")
                    emit(emptyList()) 
                },
                _sortOption,
                _sortDirection
            ) { members, sortOption, direction ->
                logManager.d("MembersViewModel", "Processing member list update. Total members: ${members.size}, Sort: $sortOption, Direction: $direction")
                
                val comparator: Comparator<Member> = when (sortOption) {
                    SortOption.ID -> compareBy { it.id }
                    SortOption.NAME -> compareBy { "${it.firstName} ${it.lastName}" }
                    SortOption.EXPIRY_DATE -> compareBy { it.expiryDate }
                }
                
                when (direction) {
                    SortDirection.ASCENDING -> members.sortedWith(comparator)
                    SortDirection.DESCENDING -> members.sortedWith(comparator.reversed())
                }
            }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
            )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Removed init block so it doesn't auto-refresh on launch

    fun refreshMembers() {
        logManager.i("MembersViewModel", "Starting member list refresh")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.refreshMembers().onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to refresh members"
                    logManager.e("MembersViewModel", "Member refresh failed: $errorMsg")
                    _error.value = errorMsg
                }.onSuccess { members ->
                    logManager.i("MembersViewModel", "Member refresh successful. Total members: ${members.size}")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "An unexpected error occurred"
                logManager.e("MembersViewModel", "Unexpected error during member refresh: $errorMsg")
                _error.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to change the current sort option
    fun setSortOption(option: SortOption) {
        // If selecting the same option, toggle the direction instead
        if (_sortOption.value == option) {
            logManager.d("MembersViewModel", "Same sort option selected, toggling direction")
            toggleSortDirection()
        } else {
            logManager.i("MembersViewModel", "Sort option changed from ${_sortOption.value} to $option")
            _sortOption.value = option
            _sortDirection.value = SortDirection.ASCENDING // Reset to ascending when changing options
        }
    }
    
    // Function to toggle between ascending and descending sort
    fun toggleSortDirection() {
        val newDirection = if (_sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
        logManager.i("MembersViewModel", "Sort direction changed from ${_sortDirection.value} to $newDirection")
        _sortDirection.value = newDirection
    }
}
