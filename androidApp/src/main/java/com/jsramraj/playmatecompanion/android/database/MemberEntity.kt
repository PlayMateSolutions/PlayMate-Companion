package com.jsramraj.playmatecompanion.android.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jsramraj.playmatecompanion.android.members.Member
import java.util.Date

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey
    val id: Long,
    val rowNumber: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val place: String,
    val joinDate: Date,
    val status: String,
    val expiryDate: Date,
    val notes: String?
) {
    // Convert from Entity to Domain model
    fun toMember(): Member {
        return Member(
            rowNumber = rowNumber,
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            place = place,
            _joinDate = joinDate.toIsoString(),
            status = status,
            _expiryDate = expiryDate.toIsoString(),
            notes = notes
        )
    }
    
    companion object {
        // Convert from Domain model to Entity
        fun fromMember(member: Member): MemberEntity {
            return MemberEntity(
                id = member.id,
                rowNumber = member.rowNumber,
                firstName = member.firstName,
                lastName = member.lastName,
                email = member.email,
                phone = member.phone,
                place = member.place,
                joinDate = member.joinDate,
                status = member.status,
                expiryDate = member.expiryDate,
                notes = member.notes
            )
        }
    }
}

// Extension function to convert Date to ISO string format
private fun Date.toIsoString(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return formatter.format(this)
}
