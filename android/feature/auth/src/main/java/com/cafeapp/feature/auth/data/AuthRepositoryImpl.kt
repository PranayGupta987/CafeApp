package com.cafeapp.feature.auth.data

import com.cafeapp.core.common.CafeDispatchers
import com.cafeapp.core.common.CafeResult
import com.cafeapp.core.common.Dispatcher
import com.cafeapp.core.datastore.SessionPreferences
import com.cafeapp.core.network.AuthApi
import com.cafeapp.core.network.ErrorResponseDto
import com.cafeapp.core.network.LoginRequest
import com.cafeapp.core.network.RefreshRequest
import com.cafeapp.core.network.RegisterRequest
import com.cafeapp.feature.auth.domain.AuthRepository
import com.cafeapp.feature.auth.domain.AuthUser
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionPreferences: SessionPreferences,
    private val moshi: Moshi,
    @Dispatcher(CafeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): CafeResult<AuthUser> = withContext(ioDispatcher) {
        try {
            val response = authApi.register(
                RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                )
            )
            val user = response.user
            val domainUser = AuthUser(
                id = user.id,
                email = user.email,
                name = user.name ?: name,
                role = user.role,
            )
            sessionPreferences.saveAuth(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: "",
                userId = domainUser.id,
                email = domainUser.email,
                name = domainUser.name,
                role = domainUser.role,
            )
            CafeResult.Success(domainUser)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun login(
        email: String,
        password: String,
    ): CafeResult<AuthUser> = withContext(ioDispatcher) {
        try {
            val response = authApi.login(
                LoginRequest(
                    email = email,
                    password = password,
                )
            )
            val user = response.user
            val domainUser = AuthUser(
                id = user.id,
                email = user.email,
                name = user.name ?: "User",
                role = user.role,
            )
            sessionPreferences.saveAuth(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: "",
                userId = domainUser.id,
                email = domainUser.email,
                name = domainUser.name,
                role = domainUser.role,
            )
            CafeResult.Success(domainUser)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun refresh(refreshToken: String): CafeResult<String> = withContext(ioDispatcher) {
        try {
            val response = authApi.refresh(RefreshRequest(refreshToken))
            sessionPreferences.saveAuthTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: refreshToken,
            )
            CafeResult.Success(response.accessToken)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun logout(): CafeResult<Unit> = withContext(ioDispatcher) {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Even if network logout fails, always clear local credentials
        } finally {
            sessionPreferences.clearAll()
        }
        CafeResult.Success(Unit)
    }

    override suspend fun getMe(): CafeResult<AuthUser> = withContext(ioDispatcher) {
        try {
            val response = authApi.getMe()
            val user = response.user
            val domainUser = AuthUser(
                id = user.id,
                email = user.email,
                name = user.name ?: "User",
                role = user.role,
            )
            sessionPreferences.saveUserInfo(
                userId = domainUser.id,
                role = domainUser.role,
                name = domainUser.name,
                email = domainUser.email,
            )
            CafeResult.Success(domainUser)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun observeUser(): Flow<AuthUser?> {
        return combine(
            sessionPreferences.userId,
            sessionPreferences.userEmail,
            sessionPreferences.userName,
            sessionPreferences.userRole,
        ) { id, email, name, role ->
            if (id != null && email != null && role != null) {
                AuthUser(
                    id = id,
                    email = email,
                    name = name ?: "User",
                    role = role,
                )
            } else {
                null
            }
        }
    }

    override suspend fun checkSession(): Boolean = withContext(ioDispatcher) {
        val accessToken = sessionPreferences.accessToken.firstOrNull()
        val refreshToken = sessionPreferences.refreshToken.firstOrNull()
        if (accessToken.isNullOrBlank() && refreshToken.isNullOrBlank()) {
            return@withContext false
        }
        // Validate with backend /auth/me
        when (getMe()) {
            is CafeResult.Success -> true
            else -> false
        }
    }

    private fun <T> handleException(e: Exception): CafeResult<T> {
        val errorMessage = when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val adapter = moshi.adapter(ErrorResponseDto::class.java)
                    val parsed = errorBody?.let { adapter.fromJson(it) }
                    parsed?.error ?: parsed?.message ?: "Server error (${e.code()})"
                } catch (_: Exception) {
                    "Request failed with error ${e.code()}"
                }
            }
            is IOException -> "Network connection error. Please verify backend is reachable."
            else -> e.localizedMessage ?: "An unexpected error occurred"
        }
        return CafeResult.Error(e, errorMessage)
    }
}
