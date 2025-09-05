package com.jsramraj.playmatecompanion.android.navigation

sealed class Route(val route: String) {
    object Main : Route("main")
    object Settings : Route("settings")
    object Members : Route("members")
}

object NavigationArgs {
    const val FROM_LOGIN = "fromLogin"
}
