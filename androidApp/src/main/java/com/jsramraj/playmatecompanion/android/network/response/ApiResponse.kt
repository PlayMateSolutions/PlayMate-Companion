package com.jsramraj.playmatecompanion.android.network.response

data class ApiResponse<T>(
    val status: String,
    val data: T
)
