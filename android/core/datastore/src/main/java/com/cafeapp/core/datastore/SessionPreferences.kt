package com.cafeapp.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "cafe_session")

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_SESSION_ID = stringPreferencesKey("current_session_id")
        private val KEY_TABLE_ID = stringPreferencesKey("current_table_id")
        private val KEY_TABLE_NUMBER = intPreferencesKey("current_table_number")
    }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { it[KEY_ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { it[KEY_REFRESH_TOKEN] }

    val userId: Flow<String?> = context.dataStore.data
        .map { it[KEY_USER_ID] }

    val userEmail: Flow<String?> = context.dataStore.data
        .map { it[KEY_USER_EMAIL] }

    val userName: Flow<String?> = context.dataStore.data
        .map { it[KEY_USER_NAME] }

    val userRole: Flow<String?> = context.dataStore.data
        .map { it[KEY_USER_ROLE] }

    val currentSessionId: Flow<String?> = context.dataStore.data
        .map { it[KEY_SESSION_ID] }

    val currentTableId: Flow<String?> = context.dataStore.data
        .map { it[KEY_TABLE_ID] }

    val currentTableNumber: Flow<Int?> = context.dataStore.data
        .map { it[KEY_TABLE_NUMBER] }

    suspend fun saveTable(tableId: String, tableNumber: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TABLE_ID] = tableId
            prefs[KEY_TABLE_NUMBER] = tableNumber
        }
    }

    suspend fun clearTable() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TABLE_ID)
            prefs.remove(KEY_TABLE_NUMBER)
        }
    }

    suspend fun saveAuth(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        name: String?,
        role: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            if (name != null) prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_ROLE] = role
        }
    }

    suspend fun saveAuthTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUserInfo(userId: String, role: String, name: String? = null, email: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_ROLE] = role
            if (name != null) prefs[KEY_USER_NAME] = name
            if (email != null) prefs[KEY_USER_EMAIL] = email
        }
    }

    suspend fun saveSessionId(sessionId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
        }
    }

    suspend fun clearSessionId() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_ID)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    /** Blocking read for OkHttp interceptor/authenticator thread. */
    fun getAccessTokenBlocking(): String? = runBlocking {
        accessToken.firstOrNull()
    }

    fun getRefreshTokenBlocking(): String? = runBlocking {
        refreshToken.firstOrNull()
    }

    fun getUserRoleBlocking(): String? = runBlocking {
        userRole.firstOrNull()
    }

    fun saveTokensBlocking(accessToken: String, refreshToken: String) = runBlocking {
        saveAuthTokens(accessToken, refreshToken)
    }

    fun clearTokensBlocking() = runBlocking {
        clearAll()
    }
}
