package com.cafeapp.core.network

import com.cafeapp.core.network.dto.SingleTableResponseDto
import com.cafeapp.core.network.dto.TablesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TablesApi {
    @GET("tables")
    suspend fun getTables(
        @Query("includeInactive") includeInactive: Boolean? = null
    ): TablesResponseDto

    @GET("tables/qr/{qrToken}")
    suspend fun getTableByQrToken(
        @Path("qrToken") qrToken: String
    ): SingleTableResponseDto

    @GET("tables/{id}")
    suspend fun getTableById(
        @Path("id") id: String
    ): SingleTableResponseDto
}
