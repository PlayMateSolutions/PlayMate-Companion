package com.jsramraj.playmatecompanion

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform