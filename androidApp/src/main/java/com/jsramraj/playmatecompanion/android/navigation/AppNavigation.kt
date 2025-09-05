package com.jsramraj.playmatecompanion.android.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jsramraj.playmatecompanion.android.auth.SessionManager
import com.jsramraj.playmatecompanion.android.main.HomeScreen
import com.jsramraj.playmatecompanion.android.members.MembersScreen
import com.jsramraj.playmatecompanion.android.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    NavHost(
        navController = navController,
        startDestination = if (sessionManager.getSportsClubId().isNullOrEmpty()) 
            Route.Settings.route 
        else 
            Route.Main.route
    ) {
        composable(
            route = Route.Main.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        composable(
            route = Route.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            arguments = listOf(
                navArgument(NavigationArgs.FROM_LOGIN) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val fromLogin = backStackEntry.arguments?.getBoolean(NavigationArgs.FROM_LOGIN) ?: false
            
            SettingsScreen(
                onLogout = onLogout,
                onSave = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Main.route) { inclusive = true }
                    }
                },
                onBack = { 
                    if (!sessionManager.getSportsClubId().isNullOrEmpty() && !fromLogin) {
                        navController.navigateUp()
                    }
                },
                onNavigateToMembers = {
                    navController.navigate(Route.Members.route)
                }
            )
        }

        composable(
            route = Route.Members.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            MembersScreen(
                onBack = { navController.navigateUp() }
            )
        }
    }
}
