package com.cafeapp.core.network

import retrofit2.http.GET

data class HealthResponse(
    val status: String,
    val timestamp: String,
)

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): HealthResponse
}
