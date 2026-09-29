package com.cafeapp.feature.auth.domain

import com.cafeapp.core.common.CafeResult
import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
)

interface AuthRepository {
    suspend fun register(name: String, email: String, password: String): CafeResult<AuthUser>
    suspend fun login(email: String, password: String): CafeResult<AuthUser>
    suspend fun refresh(refreshToken: String): CafeResult<String>
    suspend fun logout(): CafeResult<Unit>
    suspend fun getMe(): CafeResult<AuthUser>
    fun observeUser(): Flow<AuthUser?>
    suspend fun checkSession(): Boolean
}
