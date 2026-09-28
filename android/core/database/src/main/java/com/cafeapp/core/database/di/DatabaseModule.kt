package com.cafeapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.cafeapp.core.database.CafeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CafeDatabase {
        return Room.databaseBuilder(
            context,
            CafeDatabase::class.java,
            "cafeapp.db",
        ).build()
    }
}
