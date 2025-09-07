package com.jsramraj.playmatecompanion.android.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Members : Screen("members")
    object Attendance : Screen("attendance")
}
