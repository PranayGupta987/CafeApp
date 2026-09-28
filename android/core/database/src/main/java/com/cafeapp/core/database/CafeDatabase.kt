package com.cafeapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for local menu/session cache.
 * Entities will be added in Phase 2+.
 */
@Database(entities = [], version = 1, exportSchema = false)
abstract class CafeDatabase : RoomDatabase()
