package com.jsramraj.playmatecompanion.android.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var googleSignInClient: GoogleSignInClient? = null

    fun initGoogleSignIn(context: Context, clientId: String) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
//                .requestProfile()
//                .requestId()
                // .requestServerAuthCode(clientId) // This will help verify the configuration
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            // Clear any previous sign-in state
            googleSignInClient?.signOut()
            
            // Log the configuration for debugging
            println("""
                ======== Google Sign-In Config ========
                Package Name: ${context.packageName}
                Client ID: $clientId
                ===================================""".trimIndent())
        } catch (e: Exception) {
            println("""
                ======== Google Sign-In Init Error ========
                Error: ${e.message}
                ========================================""".trimIndent())
            e.printStackTrace()
        }
    }

    fun getSignInIntent(): android.content.Intent? {
        return googleSignInClient?.signInIntent
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true)
                val account = task.await()
                _authState.value = _authState.value.copy(
                    isSignedIn = true,
                    isLoading = false,
                    userEmail = account.email,
                    userName = account.displayName,
                    error = null
                )
            } catch (e: ApiException) {
                val statusName = when (e.statusCode) {
                    GoogleSignInStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "SIGN_IN_FAILED"
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "SIGN_IN_CANCELLED"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "SIGN_IN_CURRENTLY_IN_PROGRESS"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "SIGN_IN_REQUIRED"
                    else -> "UNKNOWN"
                }

                println("""
                    ======== Google Sign-In Error ========
                    Status Code: ${e.statusCode} ($statusName)
                    Message: ${e.message}
                    Has Resolution: ${e.status.hasResolution()}
                    ====================================""".trimIndent())
                e.printStackTrace()

                // Show a generic error message to the user
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign in is already in progress"
                    else -> "Unable to sign in. Please try again."
                }
                
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                googleSignInClient?.signOut()?.await()
                _authState.value = AuthState()
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    error = "Sign out failed: ${e.message}"
                )
            }
        }
    }

    fun checkExistingSignIn(activity: Activity) {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null) {
            _authState.value = _authState.value.copy(
                isSignedIn = true,
                userEmail = account.email,
                userName = account.displayName
            )
        }
    }
}
