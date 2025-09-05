package com.jsramraj.playmatecompanion.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jsramraj.playmatecompanion.android.auth.AuthScreen
import com.jsramraj.playmatecompanion.android.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }

                    if (!isAuthenticated) {
                        AuthScreen(
                            onSignInSuccess = {
                                isAuthenticated = true
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            onLogout = {
                                isAuthenticated = false
                            }
                        )
                    }
                }
            }
        }
    }
}
