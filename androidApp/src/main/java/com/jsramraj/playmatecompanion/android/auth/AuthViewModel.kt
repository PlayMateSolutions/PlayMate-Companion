package com.jsramraj.playmatecompanion.android.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.jsramraj.playmatecompanion.android.core.Constants
import com.jsramraj.playmatecompanion.android.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var logManager: LogManager? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var sessionManager: SessionManager? = null

    fun initialize(context: Context) {
        logManager = LogManager.getInstance(context)
        logManager?.i("Auth", "Initializing Google Sign-In")
        
        // Initialize session manager
        sessionManager = SessionManager(context)
        
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Constants.GOOGLE_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .requestScopes(
                    Scope("https://www.googleapis.com/auth/drive.file")
//                    Scope("https://www.googleapis.com/auth/spreadsheets"),
//                    Scope("https://www.googleapis.com/auth/script.external_request")
                )
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            
            // Check for existing session
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null) {
                // Directly update the auth state since we already have the account
                _authState.value = _authState.value.copy(
                    isSignedIn = true,
                    isLoading = false,
                    userEmail = lastAccount.email,
                    userName = lastAccount.displayName,
                    error = null
                )
                sessionManager?.saveSession(lastAccount)
            }
        } catch (e: Exception) {
            logManager?.e("Auth", "Google Sign-In initialization failed: ${e.message}")
        }
    }

    fun getSignInIntent(): android.content.Intent? = googleSignInClient?.signInIntent

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true)
                val account = task.await()
                logManager?.i("Auth", "Sign-in successful for: ${account.email}")
                sessionManager?.saveSession(account)
                
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

                logManager?.e("Auth", """
                    Google Sign-In Error:
                    Status Code: ${e.statusCode} ($statusName)
                    Message: ${e.message}
                    Has Resolution: ${e.status.hasResolution()}""".trimIndent())

                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign in is already in progress"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Session expired. Please sign in again."
                    else -> "Unable to sign in. Please try again."
                }
                
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isSignedIn = false,
                    error = errorMessage
                )
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                logManager?.i("Auth", "Signing out")
                googleSignInClient?.signOut()?.await()
                sessionManager?.clearSession()
                _authState.value = AuthState()
                onComplete()
            } catch (e: Exception) {
                logManager?.e("Auth", "Sign out failed: ${e.message}")
                _authState.value = _authState.value.copy(
                    error = "Failed to sign out: ${e.message}"
                )
                onComplete()
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            try {
                logManager?.i("Auth", "Starting token refresh")
                val account = googleSignInClient?.silentSignIn()?.await()
                if (account != null) {
                    logManager?.i("Auth", "Token refresh successful for: ${account.email}")
                    sessionManager?.saveSession(account)
                    _authState.value = _authState.value.copy(
                        isSignedIn = true,
                        userEmail = account.email,
                        userName = account.displayName,
                        error = null
                    )
                } else {
                    logManager?.w("Auth", "Silent sign-in returned null account")
                    _authState.value = _authState.value.copy(
                        isSignedIn = false,
                        error = "Session expired. Please sign in again."
                    )
                }
            } catch (e: Exception) {
                logManager?.e("Auth", "Token refresh failed: ${e.message}")
                _authState.value = _authState.value.copy(
                    isSignedIn = false,
                    error = "Session expired. Please sign in again."
                )
            }
        }
    }
}
