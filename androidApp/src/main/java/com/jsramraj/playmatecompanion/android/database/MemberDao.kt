package com.jsramraj.playmatecompanion.android.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY rowNumber")
    fun getAllMembers(): Flow<List<MemberEntity>>
    
    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Int): MemberEntity?
    
    @Query("SELECT * FROM members WHERE status LIKE '%' || :status || '%'")
    fun getMembersByStatus(status: String): Flow<List<MemberEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)
    
    @Delete
    suspend fun deleteMember(member: MemberEntity)
    
    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
    
    @Transaction
    suspend fun refreshMembers(members: List<MemberEntity>) {
        deleteAllMembers()
        insertMembers(members)
    }
}
