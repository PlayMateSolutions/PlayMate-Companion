package com.jsramraj.playmatecompanion.android.repository

import android.content.Context
import com.jsramraj.playmatecompanion.android.database.AppDatabase
import com.jsramraj.playmatecompanion.android.database.MemberEntity
import com.jsramraj.playmatecompanion.android.members.Member
import com.jsramraj.playmatecompanion.android.network.NetworkHelper
import com.jsramraj.playmatecompanion.android.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MemberRepository(
    context: Context
) {
    private val memberDao = AppDatabase.getDatabase(context).memberDao()
    private val networkHelper = NetworkHelper(context)
    private val preferencesManager = PreferencesManager(context)
    
    // Get all members from local database as a Flow
    fun getAllMembers(): Flow<List<Member>> {
        return memberDao.getAllMembers()
            .map { entities ->
                entities.map { it.toMember() }
            }
            .flowOn(Dispatchers.IO)
    }
    
    // Get members by status
    fun getMembersByStatus(status: String): Flow<List<Member>> {
        return memberDao.getMembersByStatus(status)
            .map { entities ->
                entities.map { it.toMember() }
            }
            .flowOn(Dispatchers.IO)
    }
    
    // Refresh members from network and update local database
    suspend fun refreshMembers(): Result<List<Member>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = networkHelper.fetchMembers()
                
                if (result.isSuccess) {
                    val members = result.getOrThrow()
                    // Convert and save to database
                    val memberEntities = members.map { MemberEntity.fromMember(it) }
                    memberDao.refreshMembers(memberEntities)
                    preferencesManager.lastMemberSyncTime = System.currentTimeMillis()
                }
                
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Find member ID by identifier (either ID or phone number)
    suspend fun findMemberIdByIdentifier(identifier: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Use the new DAO method to find a member by either ID or phone in a single query
                val member = memberDao.findMemberByIdOrPhone(identifier)
                
                if (member != null) {
                    return@withContext Result.success(member.id)
                }
                
                // If not found by either method
                Result.failure(Exception("No member found with ID or phone: $identifier"))
            } catch (e: Exception) {
                Result.failure(Exception("Error finding member: ${e.message}"))
            }
        }
    }
    
    // Get member by ID
    suspend fun getMemberById(id: Long): Member? {
        return withContext(Dispatchers.IO) {
            memberDao.getMemberById(id)?.toMember()
        }
    }
    
    // Get member by identifier (either ID or phone number)
    suspend fun getMemberByIdentifier(identifier: String): Member? {
        return withContext(Dispatchers.IO) {
            memberDao.findMemberByIdOrPhone(identifier)?.toMember()
        }
    }
}
