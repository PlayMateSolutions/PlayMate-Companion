package com.jsramraj.playmatecompanion.android.utils

import androidx.compose.ui.graphics.Color

object MembershipStatusUtil {
    private val ERROR_RED = Color(0xFFDC2626) // Material Red 600
    private val WARNING_AMBER = Color(0xFFF57C00) // Material Amber 700
    private val VALID_GREEN = Color(0xFF4CAF50) // Material Green 500

    fun getStatusColor(daysUntilExpiry: Int): Color = when {
        daysUntilExpiry < 0 -> ERROR_RED // Red for expired
        daysUntilExpiry <= 7 -> WARNING_AMBER // Amber for expiring within 7 days
        else -> VALID_GREEN // Green for valid membership
    }
}
