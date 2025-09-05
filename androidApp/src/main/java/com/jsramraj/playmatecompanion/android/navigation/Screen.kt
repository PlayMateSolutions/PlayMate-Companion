package com.jsramraj.playmatecompanion.android.navigation

sealed class Screen {
    object Main : Screen()
    object Settings : Screen()
    object Members : Screen()
}
