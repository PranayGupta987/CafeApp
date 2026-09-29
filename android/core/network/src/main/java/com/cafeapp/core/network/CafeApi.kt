package com.cafeapp.core.network

import com.cafeapp.core.network.dto.CafeResponseDto
import retrofit2.http.GET

interface CafeApi {
    @GET("cafe")
    suspend fun getCafeProfile(): CafeResponseDto
}
