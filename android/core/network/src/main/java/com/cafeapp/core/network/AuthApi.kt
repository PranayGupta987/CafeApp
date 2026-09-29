package com.cafeapp.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "role") val role: String? = null,
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    @Json(name = "refreshToken") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "role") val role: String,
    @Json(name = "isActive") val isActive: Boolean? = true,
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "user") val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class MeResponseDto(
    @Json(name = "user") val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class LogoutResponseDto(
    @Json(name = "ok") val ok: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class ErrorResponseDto(
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null,
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponseDto

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout(): LogoutResponseDto

    @GET("auth/me")
    suspend fun getMe(): MeResponseDto
}
