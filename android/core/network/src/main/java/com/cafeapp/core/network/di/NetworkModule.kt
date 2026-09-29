package com.cafeapp.core.network.di

import com.cafeapp.core.network.AuthApi
import com.cafeapp.core.network.AuthInterceptor
import com.cafeapp.core.network.BaseUrl
import com.cafeapp.core.network.HealthApi
import com.cafeapp.core.network.TokenAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = "http://10.0.2.2:3000/api/v1/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        @BaseUrl baseUrl: String,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi {
        return retrofit.create(HealthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCafeApi(retrofit: Retrofit): com.cafeapp.core.network.CafeApi {
        return retrofit.create(com.cafeapp.core.network.CafeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuApi(retrofit: Retrofit): com.cafeapp.core.network.MenuApi {
        return retrofit.create(com.cafeapp.core.network.MenuApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOffersApi(retrofit: Retrofit): com.cafeapp.core.network.OffersApi {
        return retrofit.create(com.cafeapp.core.network.OffersApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReservationsApi(retrofit: Retrofit): com.cafeapp.core.network.ReservationsApi {
        return retrofit.create(com.cafeapp.core.network.ReservationsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrdersApi(retrofit: Retrofit): com.cafeapp.core.network.OrdersApi {
        return retrofit.create(com.cafeapp.core.network.OrdersApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTablesApi(retrofit: Retrofit): com.cafeapp.core.network.TablesApi {
        return retrofit.create(com.cafeapp.core.network.TablesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTableSessionsApi(retrofit: Retrofit): com.cafeapp.core.network.TableSessionsApi {
        return retrofit.create(com.cafeapp.core.network.TableSessionsApi::class.java)
    }
}
