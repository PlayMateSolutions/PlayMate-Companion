package com.jsramraj.playmatecompanion.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "PlayMateSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_ID_TOKEN = "idToken"
        private const val KEY_SPORTS_CLUB_ID = "sportsClubId"
    }

    fun saveSession(account: GoogleSignInAccount) {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, account.email)
            putString(KEY_USER_NAME, account.displayName)
            putString(KEY_ID_TOKEN, account.idToken)
        }
    }

    fun clearSession() {
        prefs.edit {
            clear()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getSessionInfo(): AuthState {
        return if (isLoggedIn()) {
            AuthState(
                isSignedIn = true,
                userEmail = prefs.getString(KEY_USER_EMAIL, null),
                userName = prefs.getString(KEY_USER_NAME, null)
            )
        } else {
            AuthState()
        }
    }

    fun getIdToken(): String? = prefs.getString(KEY_ID_TOKEN, null)

    fun getSportsClubId(): String? = prefs.getString(KEY_SPORTS_CLUB_ID, null)

    fun saveSportsClubId(id: String) {
        prefs.edit {
            putString(KEY_SPORTS_CLUB_ID, id)
        }
    }
}
