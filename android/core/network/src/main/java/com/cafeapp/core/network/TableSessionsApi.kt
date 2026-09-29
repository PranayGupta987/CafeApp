package com.cafeapp.core.network

import com.cafeapp.core.network.dto.CheckInRequest
import com.cafeapp.core.network.dto.TableSessionResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TableSessionsApi {
    @GET("table-sessions/active")
    suspend fun getActiveSession(
        @Query("tableId") tableId: String? = null
    ): TableSessionResponseDto

    @POST("table-sessions/check-in")
    suspend fun checkIn(
        @Body request: CheckInRequest
    ): TableSessionResponseDto

    @POST("table-sessions/check-out/{id}")
    suspend fun checkOut(
        @Path("id") id: String
    ): TableSessionResponseDto
}
