package com.jsramraj.playmatecompanion.android.members

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
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
    
    // Current sort option
    private val _sortOption = MutableStateFlow(SortOption.ID)
    val sortOption: StateFlow<SortOption> = _sortOption
    
    // Current sort direction
    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection
    
    // Stream of members from the database, sorted according to the current sort option and direction
    val members: StateFlow<List<Member>> =
            combine(
                repository.getAllMembers().catch { emit(emptyList()) },
                _sortOption,
                _sortDirection
            ) { members, sortOption, direction ->
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
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.refreshMembers().onFailure { exception ->
                    _error.value = exception.message ?: "Failed to refresh members"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to change the current sort option
    fun setSortOption(option: SortOption) {
        // If selecting the same option, toggle the direction instead
        if (_sortOption.value == option) {
            toggleSortDirection()
        } else {
            _sortOption.value = option
            _sortDirection.value = SortDirection.ASCENDING // Reset to ascending when changing options
        }
    }
    
    // Function to toggle between ascending and descending sort
    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
    }
}
