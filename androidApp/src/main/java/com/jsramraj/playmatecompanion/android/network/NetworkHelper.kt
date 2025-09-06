package com.jsramraj.playmatecompanion.android.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jsramraj.playmatecompanion.android.auth.SessionManager
import com.jsramraj.playmatecompanion.android.core.Constants
import com.jsramraj.playmatecompanion.android.members.Member
import com.jsramraj.playmatecompanion.android.network.response.ApiResponse
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkHelper(private val context: Context) {
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    private val sessionManager = SessionManager(context)

    suspend fun fetchMembers(): Result<List<Member>> =
            withContext(Dispatchers.IO) {
                try {
                    val clubId = sessionManager.getSportsClubId()
                    var token = sessionManager.getIdToken()

                    if (clubId.isNullOrEmpty() || token.isNullOrEmpty()) {
                        return@withContext Result.failure(
                                IllegalStateException("Missing clubId or auth token")
                        )
                    }

                    val url =
                            URL(
                                    "${Constants.BASE_URL}?sportsClubId=$clubId&action=getMembers&authorization=Bearer $token"
                            )
                    val connection = url.openConnection() as HttpURLConnection

                    connection.apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Content-Type", "application/json")
                        instanceFollowRedirects = true
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }

//                    val apiResponse = gson.fromJson(response, ApiResponse::class.java)
                    val apiResponse: ApiResponse<List<Member>> = gson.fromJson(
                        response,
                        object : TypeToken<ApiResponse<List<Member>>>() {}.type
                    )

                    if (apiResponse.status == "success") {
                        val members =
                                gson.fromJson(
                                                gson.toJson(apiResponse.data),
                                                Array<Member>::class.java
                                        )
                                        .toList()
                        Result.success(members)
                    } else {
                        Result.failure(Exception("API returned error status"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
