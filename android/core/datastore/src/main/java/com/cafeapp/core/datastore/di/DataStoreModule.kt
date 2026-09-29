package com.cafeapp.core.datastore.di

import com.cafeapp.core.common.TokenProvider
import com.cafeapp.core.datastore.SessionPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTokenProvider(prefs: SessionPreferences): TokenProvider {
        return object : TokenProvider {
            override fun getAccessToken(): String? = prefs.getAccessTokenBlocking()
            override fun getRefreshToken(): String? = prefs.getRefreshTokenBlocking()
            override fun saveTokens(accessToken: String, refreshToken: String) = prefs.saveTokensBlocking(accessToken, refreshToken)
            override fun clearTokens() = prefs.clearTokensBlocking()
        }
    }
}
