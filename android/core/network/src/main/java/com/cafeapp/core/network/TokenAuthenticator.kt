package com.cafeapp.core.network

import com.cafeapp.core.common.TokenProvider
import com.squareup.moshi.Moshi
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val moshi: Moshi,
    @BaseUrl private val baseUrl: String,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("auth/login") ||
            requestUrl.contains("auth/register") ||
            requestUrl.contains("auth/refresh")
        ) {
            return null
        }

        // Avoid infinite retry loops if we already retried with same header
        if (responseCount(response) >= 3) {
            return null
        }

        synchronized(lock) {
            val currentToken = tokenProvider.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If token has already been refreshed by another concurrent thread, retry with currentToken
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenProvider.getRefreshToken() ?: run {
                tokenProvider.clearTokens()
                return null
            }

            // Call refresh endpoint synchronously
            val refreshRequestBody = "{\"refreshToken\":\"$refreshToken\"}"
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val refreshRequest = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/auth/refresh")
                .post(refreshRequestBody)
                .build()

            val rawClient = OkHttpClient.Builder().build()
            return try {
                val refreshResponse = rawClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    val adapter = moshi.adapter(AuthResponseDto::class.java)
                    val authDto = responseBody?.let { adapter.fromJson(it) }
                    if (authDto != null) {
                        val newAccess = authDto.accessToken
                        val newRefresh = authDto.refreshToken ?: refreshToken
                        tokenProvider.saveTokens(newAccess, newRefresh)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .build()
                    } else {
                        tokenProvider.clearTokens()
                        null
                    }
                } else {
                    tokenProvider.clearTokens()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
