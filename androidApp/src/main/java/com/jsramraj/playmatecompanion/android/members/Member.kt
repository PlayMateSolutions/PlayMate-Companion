package com.jsramraj.playmatecompanion.android.members

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Member(
    val rowNumber: Long,
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val place: String,
    @SerializedName("joinDate")
    private val _joinDate: String,
    val status: String,
    @SerializedName("expiryDate")
    private val _expiryDate: String,
    val notes: String?
) {
    val isActive: Boolean
        get() = status.equals("Active", ignoreCase = true)
        
    // Parse ISO 8601 date string to Date object
    val joinDate: Date
        get() = parseIsoDate(_joinDate)
        
    val expiryDate: Date
        get() = parseIsoDate(_expiryDate)
        
    // Helper function to parse ISO 8601 date strings
    private fun parseIsoDate(dateString: String): Date {
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            formatter.parse(dateString) ?: Date()
        } catch (e: Exception) {
            // Fallback to current date if parsing fails
            Date()
        }
    }
}
