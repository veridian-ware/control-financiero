package com.controlfinanciero.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.dataStore by preferencesDataStore(name = "session")

/** Persiste el token de sesión y el email del usuario logueado. */
class SessionManager(private val context: Context) {

    private val tokenKey = stringPreferencesKey("jwt_token")
    private val emailKey = stringPreferencesKey("email")

    /** Token persistido. En cada emisión sincroniza el holder en memoria del interceptor. */
    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { it[tokenKey] }
        .onEach { AuthTokenProvider.token = it }

    val emailFlow: Flow<String?> = context.dataStore.data.map { it[emailKey] }

    suspend fun save(token: String, email: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[emailKey] = email
        }
        AuthTokenProvider.token = token
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        AuthTokenProvider.token = null
    }
}
