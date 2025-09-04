package com.jsramraj.playmatecompanion.android.auth

data class AuthState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
)
