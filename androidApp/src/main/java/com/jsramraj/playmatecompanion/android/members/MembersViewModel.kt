package com.jsramraj.playmatecompanion.android.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MembersViewModel : ViewModel() {
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Replace with actual API call and database operations
                // This is just mock data for now
                _members.value = listOf(
                    Member("1", "John", "Doe", true),
                    Member("2", "Jane", "Smith", true),
                    Member("3", "Bob", "Johnson", false)
                )
            } catch (e: Exception) {
                // TODO: Handle errors
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMembers() {
        loadMembers()
    }
}
