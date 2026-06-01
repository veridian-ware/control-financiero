package com.controlfinanciero.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.auth.AuthTokenProvider
import com.controlfinanciero.data.auth.SessionManager
import com.controlfinanciero.data.models.ApiResponse
import com.controlfinanciero.data.models.AuthResponse
import com.controlfinanciero.data.models.LoginRequest
import com.controlfinanciero.data.models.RegisterRequest
import com.controlfinanciero.data.models.UpdateProfileRequest
import com.controlfinanciero.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val api = RetrofitClient.api
    private val session = SessionManager(app)

    /** null = aún cargando el estado persistido; true/false = resuelto. */
    val isAuthenticated: StateFlow<Boolean?> = session.tokenFlow
        .map<String?, Boolean?> { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Usuario autenticado actual (para el header del menú). Se carga de /me al haber token. */
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        // Un 401 en cualquier request global cierra la sesión.
        viewModelScope.launch {
            AuthTokenProvider.unauthorizedEvents.collect { session.clear() }
        }
        // Carga el usuario actual cuando hay token (también en cold start con token persistido).
        viewModelScope.launch {
            session.tokenFlow.collect { token ->
                if (token == null) _user.value = null
                else if (_user.value == null) loadCurrentUser()
            }
        }
    }

    fun login(email: String, password: String) =
        authenticate { api.login(LoginRequest(email.trim().lowercase(), password)) }

    fun register(email: String, password: String, name: String? = null) =
        authenticate {
            api.register(
                RegisterRequest(
                    email.trim().lowercase(),
                    password,
                    name?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        }

    private fun authenticate(call: suspend () -> ApiResponse<AuthResponse>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = call()
                val data = response.data
                if (response.success && data != null) {
                    _user.value = data.user
                    session.save(data.token, data.user.email)
                } else {
                    _error.value = response.message ?: "No se pudo completar la operación"
                }
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Email o contraseña incorrectos"
                    409 -> "Ya existe un usuario con ese email"
                    else -> "Error del servidor (${e.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { session.clear() }
    }

    private suspend fun loadCurrentUser() {
        try {
            val resp = api.me()
            if (resp.success) _user.value = resp.data
        } catch (_: Exception) {
            // Silencioso: el header del menú cae a las iniciales del email.
        }
    }

    /** Actualiza el nombre del perfil. onResult(true) si guardó OK. */
    fun editProfile(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val resp = api.updateProfile(UpdateProfileRequest(name.trim()))
                if (resp.success && resp.data != null) {
                    _user.value = resp.data
                    onResult(true)
                } else onResult(false)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
