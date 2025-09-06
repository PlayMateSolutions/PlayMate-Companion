package com.jsramraj.playmatecompanion.android.repository

import android.content.Context
import com.jsramraj.playmatecompanion.android.database.AppDatabase
import com.jsramraj.playmatecompanion.android.database.MemberEntity
import com.jsramraj.playmatecompanion.android.members.Member
import com.jsramraj.playmatecompanion.android.network.NetworkHelper
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
                }
                
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
