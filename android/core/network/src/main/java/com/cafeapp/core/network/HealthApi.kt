package com.cafeapp.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @field:Json(name = "status") val status: String,
    @field:Json(name = "service") val service: String? = null,
    @field:Json(name = "timestamp") val timestamp: String? = null,
    @field:Json(name = "environment") val environment: String? = null,
    @field:Json(name = "database") val database: String? = null,
    @field:Json(name = "uptime") val uptime: Double? = null,
)

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): HealthResponse
}
