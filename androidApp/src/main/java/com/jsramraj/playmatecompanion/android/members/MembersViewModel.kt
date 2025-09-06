package com.jsramraj.playmatecompanion.android.members

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsramraj.playmatecompanion.android.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MembersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemberRepository(application)

    // Stream of members from the database
    val members: StateFlow<List<Member>> =
            repository
                    .getAllMembers()
                    .catch { emit(emptyList()) }
                    .stateIn(
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
}
