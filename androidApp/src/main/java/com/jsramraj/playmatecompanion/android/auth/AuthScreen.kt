package com.jsramraj.playmatecompanion.android.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authState by viewModel.authState.collectAsState()

    // Initialize Google Sign-In with Android client configuration
    LaunchedEffect(Unit) {
        if (activity != null) {
            viewModel.initGoogleSignIn(
                context = context,
                clientId = "1031239235658-04jeuifg37vruvmkiu71m45tskj94tnv.apps.googleusercontent.com"
            )
            // Check for existing sign-in
            viewModel.checkExistingSignIn(activity)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        viewModel.handleSignInResult(task)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator()
            } else if (!authState.isSignedIn) {
                Button(
                    onClick = {
                        viewModel.getSignInIntent()?.let { signInIntent ->
                            launcher.launch(signInIntent)
                        }
                    }
                ) {
                    Text("Sign in with Google")
                }

                authState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Show signed in state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Signed in as: ${authState.userEmail}")
                    Button(
                        onClick = { viewModel.signOut() }
                    ) {
                        Text("Sign Out")
                    }
                    LaunchedEffect(Unit) {
                        onSignInSuccess()
                    }
                }
            }
        }
    }
}
